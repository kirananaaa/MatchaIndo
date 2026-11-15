package com.example.machaindo.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.machaindo.data.model.TransaksiRequest
import com.example.machaindo.data.remote.FirebaseRealtimeHelper
import com.example.machaindo.data.remote.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

enum class TransactionDatabase(val displayName: String) {
    MYSQL("MySQL"),
    FIREBASE("Firebase")
}

data class PendingTransaction(
    val trxId: String,
    val request: TransaksiRequest,
    val failedDatabases: List<TransactionDatabase>
)

data class TransactionSyncResult(
    val trxId: String,
    val failedDatabases: List<TransactionDatabase>,
    val errors: Map<TransactionDatabase, String>
)

object TransactionSyncRepository {
    private const val PREFERENCES_NAME = "pending_transaction_sync"
    private lateinit var preferences: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    }

    fun pendingTransactions(): List<PendingTransaction> {
        check(::preferences.isInitialized) { "TransactionSyncRepository belum diinisialisasi" }
        return preferences.all.values.map { value ->
            gson.fromJson(value as String, PendingTransaction::class.java)
        }
    }

    suspend fun synchronize(
        transaction: PendingTransaction,
        onProgress: suspend (Int) -> Unit = {}
    ): TransactionSyncResult {
        check(::preferences.isInitialized) { "TransactionSyncRepository belum diinisialisasi" }
        savePending(transaction)

        val totalDatabases = transaction.failedDatabases.size
        val completedDatabases = AtomicInteger()
        if (totalDatabases == 0) onProgress(100)

        val errors = coroutineScope {
            transaction.failedDatabases.map { database ->
                async(Dispatchers.IO) {
                    val error = try {
                        when (database) {
                            TransactionDatabase.MYSQL -> {
                                val response = RetrofitClient.api.submitCheckout(transaction.request)
                                if (!response.isSuccessful || response.body()?.success != true) {
                                    val errorBody = response.errorBody()?.string()
                                    val errorDetails = errorBody
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { ": $it" }
                                        .orEmpty()
                                    throw IOException(
                                        response.body()?.message
                                            ?: "HTTP ${response.code()} ${response.message()}$errorDetails"
                                    )
                                }
                            }
                            TransactionDatabase.FIREBASE ->
                                FirebaseRealtimeHelper.insert(transaction.trxId, transaction.request)
                        }
                        null
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        exception.message ?: "Terjadi kesalahan yang tidak diketahui"
                    }
                    val progress = completedDatabases.incrementAndGet() * 100 / totalDatabases
                    withContext(Dispatchers.Main.immediate) {
                        onProgress(progress)
                    }
                    database to error
                }
            }.awaitAll().toMap()
        }

        val failedDatabases = errors.filterValues { it != null }.keys.toList()
        val remaining = transaction.copy(failedDatabases = failedDatabases)
        withContext(Dispatchers.IO) {
            if (failedDatabases.isEmpty()) {
                preferences.edit().remove(transaction.trxId).apply()
            } else {
                savePending(remaining)
            }
        }

        return TransactionSyncResult(
            trxId = transaction.trxId,
            failedDatabases = failedDatabases,
            errors = errors.mapNotNull { (database, message) ->
                message?.let { database to it }
            }.toMap()
        )
    }

    private fun savePending(transaction: PendingTransaction) {
        preferences.edit().putString(transaction.trxId, gson.toJson(transaction)).apply()
    }
}

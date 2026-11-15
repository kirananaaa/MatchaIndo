package com.example.machaindo.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class TransactionRecord(
    val trxId: String,
    val detail: String,
    val total: Double,
    val metode: String,
    val time: String
)

@SuppressLint("StaticFieldLeak")
object TransactionRepository {
    private const val PREF_NAME = "matchaindo_tx_prefs"
    private const val KEY_HISTORY = "tx_history"
    private val gson = Gson()
    private lateinit var prefs: SharedPreferences
    val history = mutableListOf<TransactionRecord>()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadFromPrefs()
        if (history.isEmpty()) {
            history.add(TransactionRecord("#KK-0092841", "2x Kopi Aren Premium", 40000.0, "Tunai (Cash)", "23 Okt 2026, 09:41"))
            saveToPrefs()
        }
    }

    private fun loadFromPrefs() {
        try {
            val json = prefs.getString(KEY_HISTORY, null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<TransactionRecord>>() {}.type
                val savedList: List<TransactionRecord> = gson.fromJson(json, type)
                history.clear()
                history.addAll(savedList)
            }
        } catch (_: Exception) {}
    }

    private fun saveToPrefs() {
        try {
            val json = gson.toJson(history)
            prefs.edit().putString(KEY_HISTORY, json).apply()
        } catch (_: Exception) {}
    }

    fun addTransaction(record: TransactionRecord) {
        history.add(0, record)
        saveToPrefs()
    }
}

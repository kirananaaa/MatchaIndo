package com.example.machaindo.data.remote

import com.example.machaindo.data.model.TransaksiRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object FirebaseRealtimeHelper {
    private const val DATABASE_URL =
        "https://matcha-indonesia-default-rtdb.firebaseio.com"

    suspend fun insert(trxId: String, request: TransaksiRequest) {
        val payload = mapOf(
            "id_tran" to trxId,
            "id_cabang" to request.idCabang,
            "id_user" to request.idUser,
            "total_bayar" to request.totalBayar,
            "metode_bayar" to request.metodeBayar,
            "uang_diterima" to request.uangDiterima,
            "items" to request.items.map { item ->
                mapOf(
                    "id_menu" to item.idMenu,
                    "id_cup" to item.idCup,
                    "jumlah" to item.jumlah,
                    "harga_satuan" to item.hargaSatuan
                )
            }
        )

        FirebaseDatabase.getInstance(DATABASE_URL)
            .getReference("transaksi")
            .child(trxId.removePrefix("#"))
            .setValue(payload)
            .await()
    }
}

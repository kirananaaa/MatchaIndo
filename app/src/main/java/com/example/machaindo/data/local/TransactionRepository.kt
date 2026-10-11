package com.example.machaindo.data.local

data class TransactionRecord(
    val trxId: String,
    val detail: String,
    val total: Double,
    val metode: String,
    val time: String
)

object TransactionRepository {
    val history = mutableListOf(
        TransactionRecord("#KK-0092841", "2x Kopi Aren Premium", 40000.0, "Tunai (Cash)", "23 Okt 2026, 09:41")
    )

    fun addTransaction(record: TransactionRecord) {
        history.add(0, record)
    }
}

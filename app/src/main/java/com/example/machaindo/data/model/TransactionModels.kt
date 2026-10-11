package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class TransaksiRequest(
    @SerializedName("id_cabang") val idCabang: Long,
    @SerializedName("id_user") val idUser: Long,
    @SerializedName("total_bayar") val totalBayar: Double,
    @SerializedName("metode_bayar") val metodeBayar: String,
    @SerializedName("uang_diterima") val uangDiterima: Double?,
    val items: List<DetailTransaksiRequest>
)

data class DetailTransaksiRequest(
    @SerializedName("id_menu") val idMenu: Long,
    @SerializedName("id_cup") val idCup: Long,
    val jumlah: Int,
    @SerializedName("harga_satuan") val hargaSatuan: Double
)

data class TransaksiResponse(
    val success: Boolean,
    val message: String
)

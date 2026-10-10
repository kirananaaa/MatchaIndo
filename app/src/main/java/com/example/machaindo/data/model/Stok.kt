package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class StokItem(
    @SerializedName("id_bahan") val idBahan: Long,
    @SerializedName("nama_bahan") val namaBahan: String,
    val satuan: String,
    @SerializedName("jumlah_stok") val jumlahStok: Int,
    @SerializedName("stok_minimum") val stokMinimum: Int,
    val status: String
)

data class StokResponse(val data: List<StokItem>)
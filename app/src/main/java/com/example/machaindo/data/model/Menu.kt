package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class MenuResponse(
    val data: List<Menu>
)

data class Menu(
    @SerializedName("id_menu") val idMenu: Int,
    @SerializedName("nama_menu") val namaMenu: String,
    val kategori: String,
    @SerializedName("is_aktif") val isAktif: Boolean
)

package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class MenuResponse(
    val data: List<Menu>
)

data class Menu(
    @SerializedName("id_menu", alternate = ["id"]) val idMenu: Int,
    @SerializedName("nama_menu", alternate = ["name"]) val namaMenu: String,
    @SerializedName("kategori", alternate = ["category"]) val kategori: String,
    @SerializedName("is_aktif", alternate = ["is_active", "status"]) val isAktif: Boolean,
    @SerializedName("harga", alternate = ["price"]) val harga: Double = 20000.0,
    @SerializedName("gambar", alternate = ["image", "photo"]) val gambar: String? = null
)

data class CartItem(
    val menu: Menu,
    var quantity: Int = 1,
    val variant: String = "Medium"
)

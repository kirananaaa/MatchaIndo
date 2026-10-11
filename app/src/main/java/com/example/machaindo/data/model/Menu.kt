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
    // harga terendah dari semua ukuran cup (dari API)
    @SerializedName("harga", alternate = ["price"]) val harga: Double = 0.0,
    // harga per ukuran cup, dipakai nanti untuk pilihan ukuran di keranjang
    @SerializedName("harga_menus") val hargaMenus: List<HargaCup>? = null,
    @SerializedName("gambar", alternate = ["image", "photo"]) val gambar: String? = null
)

data class HargaCup(
    @SerializedName("id_cup") val idCup: Int,
    @SerializedName("nama_cup") val namaCup: String?,
    @SerializedName("volume_ml") val volumeMl: Int?,
    val harga: Double
)

data class CartItem(
    val menu: Menu,
    var quantity: Int = 1,
    val variant: String = "Medium"
)
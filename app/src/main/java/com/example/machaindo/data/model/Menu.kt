package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class MenuResponse(
    val data: List<Menu>
)

data class Menu(
    val id: Int,
    val name: String,
    val category: String,
    val description: String?,
    val price: Int,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_available") val isAvailable: Boolean
)

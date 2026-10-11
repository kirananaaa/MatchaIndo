package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class UserDto(
    @SerializedName("id_user") val idUser: Int,
    @SerializedName("id_cabang") val idCabang: Int? = null,
    @SerializedName("nama_cabang") val namaCabang: String?,
    val nama: String,
    val username: String,
    val role: String
)

data class LoginResponse(
    val token: String,
    val user: UserDto
)
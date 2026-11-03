package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class UserDto(
    @SerializedName("id_user", alternate = ["id"]) val idUser: Int = 1,
    @SerializedName("id_cabang", alternate = ["branch_id"]) val idCabang: Int? = null,
    @SerializedName("nama_cabang", alternate = ["branch_name"]) val namaCabang: String? = null,
    @SerializedName("nama", alternate = ["name"]) val nama: String? = "Barista",
    @SerializedName("username") val username: String? = "",
    @SerializedName("role") val role: String? = "barista"
)

data class LoginResponse(
    val token: String,
    val user: UserDto?
)

data class LoginWrapper(
    val status: Boolean,
    val message: String,
    val data: LoginResponse
)

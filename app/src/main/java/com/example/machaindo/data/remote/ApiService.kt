package com.example.machaindo.data.remote

import com.example.machaindo.data.model.AbsensiRequest
import com.example.machaindo.data.model.AbsensiResponse
import com.example.machaindo.data.model.MenuResponse
import com.example.machaindo.data.model.LoginRequest
import com.example.machaindo.data.model.LoginResponse
import com.example.machaindo.data.model.StokResponse
import com.example.machaindo.data.model.UserDto
import com.example.machaindo.data.model.TransaksiRequest
import com.example.machaindo.data.model.TransaksiResponse
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.Response
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @GET("menus")
    suspend fun getMenus(
        @Query("search") search: String? = null,
        @Query("kategori") kategori: String? = null
    ): MenuResponse

    @GET("menus")
    suspend fun getMenusJson(
        @Query("search") search: String? = null,
        @Query("kategori") kategori: String? = null
    ): JsonElement

    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("logout")
    suspend fun logout()

    @GET("user-profile")
    suspend fun getProfile(): UserDto

    @GET("stok")
    suspend fun getStok(): Response<StokResponse>

    // --- Transaksi Checkout ---
    @POST("transaksi/checkout")
    suspend fun submitCheckout(@Body request: TransaksiRequest): Response<TransaksiResponse>

    // --- Absensi ---
    @GET("absensi/hari-ini")
    suspend fun getAbsensiHariIni(): AbsensiResponse

    @POST("absensi/masuk")
    suspend fun absenMasuk(@Body body: AbsensiRequest): AbsensiResponse

    @POST("absensi/pulang")
    suspend fun absenPulang(@Body body: AbsensiRequest): AbsensiResponse
}

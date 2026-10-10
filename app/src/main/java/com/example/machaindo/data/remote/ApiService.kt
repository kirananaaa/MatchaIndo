package com.example.machaindo.data.remote

import com.example.machaindo.data.model.MenuResponse
import com.example.machaindo.data.model.LoginRequest
import com.example.machaindo.data.model.LoginResponse
import com.example.machaindo.data.model.StokResponse
import com.example.machaindo.data.model.UserDto
import retrofit2.http.GET
import retrofit2.Response
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @GET("menus")
    suspend fun getMenus(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null
    ): MenuResponse

    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("logout")
    suspend fun logout()

    @GET("user-profile")
    suspend fun getProfile(): UserDto

    @GET("stok")
    suspend fun getStok(): Response<StokResponse>
}

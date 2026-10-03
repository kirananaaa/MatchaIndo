package com.example.machaindo.data.remote

import com.example.machaindo.data.model.MenuResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("menus")
    suspend fun getMenus(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null
    ): MenuResponse
}

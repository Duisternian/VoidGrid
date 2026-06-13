package com.duisternis.voidgrid

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface GoogleSearchApi {
    @GET("/")
    suspend fun getVqdToken(
        @Query("q") query: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    ): String

    @GET("i.js")
    suspend fun getImagesJson(
        @Query("q") query: String,
        @Query("vqd") vqd: String,
        @Query("s") skip: Int, // 🟢 INDICA A PARTIR DE QUAL IMAGEM COMPLEMENTAR O FEED
        @Query("o") format: String = "json",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    ): String
}
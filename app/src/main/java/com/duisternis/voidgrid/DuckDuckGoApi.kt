package com.duisternis.voidgrid

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleSearchApi {
    @GET(".")
    suspend fun getVqdToken(
        @Query("q") query: String,
        @Query("ia") ia: String = "images",
        @Query("iax") iax: String = "images"
    ): String

    @GET("i.js")
    suspend fun getImagesJson(
        @Query("q") query: String,
        @Query("vqd") vqd: String,
        @Query("s") skip: Int = 0,
        @Query("o") format: String = "json",
        @Query("p") safeSearch: String = "-1",  // -1 = off, não -2
        @Query("f") filters: String = ",,,,,",
        @Query("l") region: String = "wt-wt"
    ): String
}
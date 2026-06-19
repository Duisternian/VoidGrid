package com.duisternis.voidgrid.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface DuckDuckGoApi {

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
        @Query("p") safeSearch: String = "1", // "1" = on, "-1" = off
        @Query("f") filters: String = ",,,,,",
        @Query("l") region: String = "wt-wt"
    ): String
}
package com.duisternis.voidgrid

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleSearchApi {
    @GET(".")
    suspend fun getVqdToken(
        @Query("q") query: String,
        @Query("ia") ia: String = "images",
        @Query("iax") iax: String = "images",
        @Query("t") t: String = "h_",
        @Query("chip-select") chipSelect: String = "search"
    ): String

    @GET("i.js")
    suspend fun getImagesJson(
        @Query("q") query: String,
        @Query("vqd") vqd: String,
        @Query("s") skip: Int = 0,
        @Query("o") format: String = "json",
        @Query("p") safeSearch: String = "-2",  // kp, não p
        @Query("f") filters: String = ",,,,,",
        @Query("l") region: String = "us-en"     // us-en, não wt-wt
    ): String
}
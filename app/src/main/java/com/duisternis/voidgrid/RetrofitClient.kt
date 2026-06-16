package com.duisternis.voidgrid

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

// ─── Configuração do Cliente HTTP ───────────────────────────────────────────

object RetrofitClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .build()
            chain.proceed(request)
        }
        .build()

    // ─── Instância da API ────────────────────────────────────────────────────

    val duckDuckGoApi: GoogleSearchApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://duckduckgo.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GoogleSearchApi::class.java)
    }
}
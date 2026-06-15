package com.duisternis.voidgrid

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

// 1. Criamos um "Local" que vai guardar o nosso ImageLoader
val LocalImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("No ImageLoader provided")
}

// 2. Criamos uma função para configurar o Singleton com as otimizações que você queria
fun createCustomImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.15) // Otimização: 15% da memória
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .crossfade(true)
        .build()
}
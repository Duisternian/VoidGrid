package com.duisternis.voidgrid

import android.app.Application
import com.duisternis.voidgrid.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

// ─── Application ─────────────────────────────────────────────────────────────

class VoidGridApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR) // ERROR em produção para não poluir o Logcat
            androidContext(this@VoidGridApplication)
            modules(appModule)
        }
    }
}
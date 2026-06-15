package com.duisternis.voidgrid

import android.content.Context
import android.content.SharedPreferences

class TokenCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "voidgrid_tokens",
        Context.MODE_PRIVATE
    )

    suspend fun getOrFetch(query: String, fetcher: suspend () -> String): String {
        val key = "vqd_${query.hashCode()}"
        val timestampKey = "time_${query.hashCode()}"

        val cached = prefs.getString(key, null)
        val timestamp = prefs.getLong(timestampKey, 0L)

        if (cached != null && System.currentTimeMillis() - timestamp < 30 * 60 * 1000) {
            return cached
        }

        val newToken = fetcher()
        prefs.edit().apply {
            putString(key, newToken)
            putLong(timestampKey, System.currentTimeMillis())
            apply()
        }
        return newToken
    }
}
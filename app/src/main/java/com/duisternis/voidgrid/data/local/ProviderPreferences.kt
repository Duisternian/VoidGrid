package com.duisternis.voidgrid.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.providerDataStore by preferencesDataStore(name = "search_provider_prefs")

class ProviderPreferences(private val context: Context) {

    private object Keys {
        val CUSTOM_DOMAINS = stringSetPreferencesKey("custom_domains")
        val SELECTED_PROVIDER_ID = stringPreferencesKey("selected_provider_id")
        val SAFE_SEARCH = booleanPreferencesKey("safe_search")
    }

    val customDomains: Flow<List<String>> =
        context.providerDataStore.data.map { prefs ->
            prefs[Keys.CUSTOM_DOMAINS]?.toList()?.sorted() ?: emptyList()
        }

    val selectedProviderId: Flow<String> =
        context.providerDataStore.data.map { prefs ->
            prefs[Keys.SELECTED_PROVIDER_ID] ?: "all"
        }

    // Default true — safesearch ligado por padrão
    val safeSearch: Flow<Boolean> =
        context.providerDataStore.data.map { prefs ->
            prefs[Keys.SAFE_SEARCH] ?: true
        }

    suspend fun addCustomDomain(domain: String) {
        val cleaned = normalizeDomain(domain)
        if (cleaned.isBlank()) return
        context.providerDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_DOMAINS] ?: emptySet()
            prefs[Keys.CUSTOM_DOMAINS] = current + cleaned
        }
    }

    suspend fun removeCustomDomain(domain: String) {
        context.providerDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_DOMAINS] ?: emptySet()
            prefs[Keys.CUSTOM_DOMAINS] = current - domain
        }
    }

    suspend fun setSelectedProviderId(id: String) {
        context.providerDataStore.edit { prefs ->
            prefs[Keys.SELECTED_PROVIDER_ID] = id
        }
    }

    suspend fun setSafeSearch(enabled: Boolean) {
        context.providerDataStore.edit { prefs ->
            prefs[Keys.SAFE_SEARCH] = enabled
        }
    }

    private fun normalizeDomain(raw: String): String {
        return raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .lowercase()
    }
}
package com.duisternis.voidgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.local.entity.PinEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ForYouViewModel(
    private val repository: ImageSearchRepository
) : ViewModel() {

    private val _activeQuery = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _activeQuery
        .flatMapLatest { query ->
            if (query.isNullOrBlank()) flowOf(PagingData.empty())
            else repository.searchImages(query, safeSearch = false)
        }
        .cachedIn(viewModelScope)

    fun updateQueriesFromPins(pins: List<PinEntity>) {
        if (pins.isEmpty()) {
            _activeQuery.value = null
            return
        }

        val dominantSource = pins
            .map { it.source }
            .filter { it.isNotBlank() && it != "unknown" }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val query = if (dominantSource != null) {
            val domain = if (dominantSource.contains(".")) dominantSource else "$dominantSource.com"
            "art site:$domain"
        } else {
            pins.map { it.source }
                .distinct()
                .take(3)
                .joinToString(" OR ")
        }

        if (query != _activeQuery.value) {
            _activeQuery.value = query
        }
    }
}
package com.duisternis.voidgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

// ─── ViewModel de busca de imagens ───────────────────────────────────────────

class ImageSearchViewModel(
    private val repository: ImageSearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow<String?>(null)
    val currentQuery: StateFlow<String?> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _query
        .filterNotNull()
        .filter { it.isNotBlank() }
        .flatMapLatest { repository.searchImages(it) }
        .cachedIn(viewModelScope)

    fun search(newQuery: String) {
        if (newQuery.isBlank() || newQuery == _query.value) return
        _query.value = newQuery
    }
}
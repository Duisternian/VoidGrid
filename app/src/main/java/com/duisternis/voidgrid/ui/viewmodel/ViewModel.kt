package com.duisternis.voidgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.api.SearchPagingSource
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

class ImageSearchViewModel(
    private val repository: ImageSearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow<String?>(null)
    val currentQuery: StateFlow<String?> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _query
        .filterNotNull()
        .filter { it.isNotBlank() }
        .flatMapLatest { query ->
            // Agora criamos o Pager aqui no ViewModel, injetando o repositório no PagingSource
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { SearchPagingSource(repository, query) }
            ).flow
        }
        .cachedIn(viewModelScope)

    fun search(newQuery: String) {
        if (newQuery.isBlank() || newQuery == _query.value) return
        _query.value = newQuery
    }
}
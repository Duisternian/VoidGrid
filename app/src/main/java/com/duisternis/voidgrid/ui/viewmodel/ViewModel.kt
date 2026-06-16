package com.duisternis.voidgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.api.RetrofitClient
import com.duisternis.voidgrid.data.api.SearchPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class ImageSearchViewModel : ViewModel() {
    private val _query = MutableStateFlow<String?>(null)
    val currentQuery: StateFlow<String?> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _query
        .filterNotNull()
        .filter { it.isNotBlank() }
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 5, enablePlaceholders = false),
                pagingSourceFactory = { SearchPagingSource(query, RetrofitClient.duckDuckGoApi) }
            ).flow
        }
        .cachedIn(viewModelScope)

    fun search(newQuery: String) { _query.value = newQuery }
}
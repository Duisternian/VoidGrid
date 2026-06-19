package com.duisternis.voidgrid.data.api

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.repository.ImageSearchRepository

class SearchPagingSource(
    private val repository: ImageSearchRepository,
    private val query: String,
    private val safeSearch: Boolean
) : PagingSource<Int, SearchItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchItem> {
        val position = params.key ?: 0
        val (items, nextS) = repository.fetchImages(query, position, safeSearch)

        return LoadResult.Page(
            data = items,
            prevKey = if (position == 0) null else position,
            nextKey = nextS
        )
    }

    override fun getRefreshKey(state: PagingState<Int, SearchItem>): Int? {
        return state.anchorPosition
    }
}
package com.duisternis.voidgrid.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.ui.components.ShimmerBox

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSearchScreen(
    pagingItems: LazyPagingItems<SearchItem>,
    onSearch: (String) -> Unit,
    imageLoader: ImageLoader,
    hasQuery: Boolean
) {
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(top = 135.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = pagingItems.itemCount,
                key = { index -> "${pagingItems.peek(index)?.link}_$index" }
            ) { index ->
                val item = pagingItems[index] ?: return@items
                var isError by remember(item.link) { mutableStateOf(false) }
                var isLoaded by remember(item.link) { mutableStateOf(false) }

                if (!isError) {
                    val aspectRatio = if (item.width > 0 && item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.75f
                    Card(modifier = Modifier.fillMaxWidth().clickable { focusManager.clearFocus(); selectedItem = item }) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)) {
                            if (!isLoaded) ShimmerBox(modifier = Modifier.fillMaxSize())
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.link.replace(" ", "%20")).crossfade(100)
                                    .diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED)
                                    .precision(Precision.INEXACT).build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                onSuccess = { isLoaded = true },
                                onError = { isError = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Barra de busca
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding())
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp),
                placeholder = { Text("Pesquisar...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query); focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF191919), unfocusedContainerColor = Color(0xFF191919),
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(25.dp)
            )
        }

        selectedItem?.let { item ->
            ImageDetailDialog(item, pagingItems.itemSnapshotList.items, { selectedItem = null }, imageLoader)
        }
    }
}
package com.duisternis.voidgrid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var currentQuery = ""
    private var currentVqd = ""
    private var isCurrentlyLoadingNextPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isSystemInDarkTheme()) Color.Black else Color.White
                ) {
                    var searchResults by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(false) }

                    ImageSearchScreen(
                        images = searchResults,
                        isLoading = isLoading,
                        onSearchTriggered = { query ->
                            currentQuery = query
                            lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) { isLoading = true }
                                try {
                                    val htmlResponse = RetrofitClient.googleSearchApi.getVqdToken(query = query)
                                    val vqdRegex = """vqd=["']?([0-9-]+)["']?""".toRegex()
                                    val vqdMatch = vqdRegex.find(htmlResponse)
                                    currentVqd = vqdMatch?.groups[1]?.value ?: ""

                                    if (currentVqd.isNotEmpty()) {
                                        val jsonResponse = RetrofitClient.googleSearchApi.getImagesJson(query = query, vqd = currentVqd, skip = 0)
                                        val items = parseDuckDuckGoJson(jsonResponse)
                                        withContext(Dispatchers.Main) { searchResults = items }
                                    }
                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Erro na busca: ${e.message}")
                                } finally {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        },
                        onLoadNextPage = { currentListSize ->
                            if (currentVqd.isNotEmpty() && !isCurrentlyLoadingNextPage) {
                                isCurrentlyLoadingNextPage = true
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonResponse = RetrofitClient.googleSearchApi.getImagesJson(query = currentQuery, vqd = currentVqd, skip = currentListSize)
                                        val newItems = parseDuckDuckGoJson(jsonResponse)
                                        withContext(Dispatchers.Main) {
                                            val novosFiltrados = newItems.filter { novo -> !searchResults.any { it.link == novo.link } }
                                            val updatedList = (searchResults + novosFiltrados)
                                            searchResults = if (updatedList.size > 100) updatedList.drop(40) else updatedList
                                        }
                                    } finally {
                                        isCurrentlyLoadingNextPage = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun parseDuckDuckGoJson(json: String): List<SearchItem> {
        val blockRegex = """"image"\s*:\s*"([^"]+)"[^}]+"source"\s*:\s*"([^"]+)"""".toRegex()
        val matches = blockRegex.findAll(json)
        return matches.map { match ->
            SearchItem(link = match.groups[1]?.value?.replace("\\/", "/") ?: "", source = match.groups[2]?.value?.lowercase() ?: "")
        }.filter { it.link.startsWith("http") }.distinctBy { it.link }.toList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSearchScreen(images: List<SearchItem>, isLoading: Boolean, onSearchTriggered: (String) -> Unit, onLoadNextPage: (Int) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {
                onSearchTriggered(searchQuery)
                focusManager.clearFocus() // Fecha o teclado após a busca
            },
            active = false,
            onActiveChange = {},
            placeholder = { Text("Pesquisar...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            colors = SearchBarDefaults.colors(
                containerColor = if (isDark) Color(0xFF121212) else Color(0xFFE0E0E0)
            )
        ) {}

        if (isLoading && images.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.Gray) }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items = images, key = { _, item -> item.link }) { index, item ->
                    if (index >= images.size - 3) { LaunchedEffect(images.size) { onLoadNextPage(images.size) } }
                    key(item.link) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color.Black else Color.White
                            )
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(item.link).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}
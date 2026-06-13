package com.duisternis.voidgrid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // Guardamos o termo atual e o token em memória para usar nas paginações adicionais
    private var currentQuery = ""
    private var currentVqd = ""
    private var isCurrentlyLoadingNextPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageSearchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var searchResults by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(false) }

                    ImageSearchScreen(
                        images = searchResults,
                        isLoading = isLoading,
                        modifier = Modifier.padding(innerPadding),
                        onSearchTriggered = { query ->
                            currentQuery = query
                            lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) { isLoading = true }
                                try {
                                    // Primeira busca: Pega o Token VQD inicial
                                    val htmlResponse = RetrofitClient.googleSearchApi.getVqdToken(query = query)
                                    val vqdRegex = """vqd=["']?([0-9-]+)["']?""".toRegex()
                                    val vqdMatch = vqdRegex.find(htmlResponse)
                                    currentVqd = vqdMatch?.groups[1]?.value ?: ""

                                    if (currentVqd.isNotEmpty()) {
                                        // Pega o lote inicial (skip = 0)
                                        val jsonResponse = RetrofitClient.googleSearchApi.getImagesJson(
                                            query = query, vqd = currentVqd, skip = 0
                                        )
                                        val items = parseDuckDuckGoJson(jsonResponse)

                                        withContext(Dispatchers.Main) {
                                            searchResults = items
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Erro na busca inicial: ${e.message}")
                                } finally {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        },
                        onLoadNextPage = { currentListSize ->
                            // Só busca mais se já tiver um token ativo e não estiver ocupado carregando
                            if (currentVqd.isNotEmpty() && !isCurrentlyLoadingNextPage) {
                                isCurrentlyLoadingNextPage = true
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        Log.d("API_INFO", "Carregando mais imagens... Pulando as primeiras $currentListSize")
                                        val jsonResponse = RetrofitClient.googleSearchApi.getImagesJson(
                                            query = currentQuery, vqd = currentVqd, skip = currentListSize
                                        )
                                        val newItems = parseDuckDuckGoJson(jsonResponse)

                                        if (newItems.isNotEmpty()) {
                                            withContext(Dispatchers.Main) {
                                                // Soma os resultados novos no final da lista existente
                                                searchResults = searchResults + newItems
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("API_ERROR", "Erro ao paginar: ${e.message}")
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

    // Função auxiliar isolada para limpar o JSON e reordenar por sites importantes
    private fun parseDuckDuckGoJson(json: String): List<SearchItem> {
        val blockRegex = """"image"\s*:\s*"([^"]+)"[^}]+"source"\s*:\s*"([^"]+)"""".toRegex()
        val matches = blockRegex.findAll(json)
        val sitesPrioritarios = listOf("amazon", "reddit", "wikipedia", "pinterest")

        val items = matches.map { match ->
            val imgUrl = match.groups[1]?.value?.replace("\\/", "/") ?: ""
            val sourceSite = match.groups[2]?.value?.lowercase() ?: ""
            SearchItem(link = imgUrl, source = sourceSite)
        }.filter { item -> item.link.startsWith("http") }
            .distinctBy { it.link }
            .toList()

        return items.sortedByDescending { item ->
            sitesPrioritarios.any { site -> item.source.contains(site) }
        }
    }
}

// 🟢 A classe duplicada "data class SearchItem" que ficava aqui foi removida com sucesso!

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSearchScreen(
    images: List<SearchItem>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onSearchTriggered: (String) -> Unit,
    onLoadNextPage: (Int) -> Unit // Callback para avisar que a rolagem chegou ao fim
) {
    var searchQuery by remember { mutableStateOf("") }
    val gridState = rememberLazyStaggeredGridState()

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { onSearchTriggered(searchQuery) },
            active = false,
            onActiveChange = {},
            placeholder = { Text("Pesquisa Infinita Estilo Pinterest...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) { }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalStaggeredGrid(
                state = gridState, // Passamos o controle de estado da Grid
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // itemsIndexed nos dá o index numérico do item atual na tela
                itemsIndexed(images) { index, item ->

                    // GATILHO INFINITO AUTOMÁTICO:
                    // Quando o usuário chegar no item de número (Tamanho da lista - 6), o app já puxa a próxima leva em background.
                    if (index >= images.size - 6 && images.size >= 15) {
                        LaunchedEffect(images.size) {
                            onLoadNextPage(images.size)
                        }
                    }

                    ImageGridItem(imageUrl = item.link)
                }
            }
        }
    }
}

@Composable
fun ImageGridItem(imageUrl: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Feed Infinito",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
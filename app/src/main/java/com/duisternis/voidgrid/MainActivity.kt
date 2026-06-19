package com.duisternis.voidgrid

// ─── Imports ─────────────────────────────────────────────────────────────────
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.compose.collectAsLazyPagingItems
import com.duisternis.voidgrid.data.util.LocalImageLoader
import com.duisternis.voidgrid.data.util.createCustomImageLoader
import com.duisternis.voidgrid.ui.screens.ImageSearchScreen
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.KoinContext

// ─── MainActivity ────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    // KOIN gerencia a injeção do ViewModel que, por sua vez, recebe o repositório
    private val viewModel: ImageSearchViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KoinContext {
            ImageSearchTheme {
                // Inicialização do ImageLoader personalizado
                val imageLoader = remember { createCustomImageLoader(this) }

                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    // Estados do Paging e Query vindo do ViewModel injetado pelo KOIN
                    val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
                    val currentQuery by viewModel.currentQuery.collectAsState()

                    // Renderização da tela principal
                    ImageSearchScreen(
                        pagingItems = pagingItems,
                        onSearch = { viewModel.search(it) },
                        imageLoader = imageLoader,
                        hasQuery = !currentQuery.isNullOrBlank()
                    )
                }
            }
        }
    }
}
    }
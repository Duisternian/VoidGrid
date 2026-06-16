package com.duisternis.voidgrid

// ─── Imports ─────────────────────────────────────────────────────────────────
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

// ─── MainActivity ────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    // ViewModel associado a esta Activity
    private val viewModel: ImageSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ImageSearchTheme {
                // Inicialização do ImageLoader personalizado
                val imageLoader = remember { createCustomImageLoader(this) }

                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    // Estados do Paging e Query
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
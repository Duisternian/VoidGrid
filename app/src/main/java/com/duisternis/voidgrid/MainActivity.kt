package com.duisternis.voidgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.util.LocalImageLoader
import com.duisternis.voidgrid.data.util.createCustomImageLoader
import com.duisternis.voidgrid.ui.components.BottomNavBar
import com.duisternis.voidgrid.ui.components.Screen
import com.duisternis.voidgrid.ui.viewmodel.ForYouScreen
import com.duisternis.voidgrid.ui.screens.ImageDetailDialog
import com.duisternis.voidgrid.ui.screens.ImageSearchScreen
import com.duisternis.voidgrid.ui.screens.ProfileScreen
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KoinContext {
                ImageSearchTheme {
                    val imageLoader = remember { createCustomImageLoader(applicationContext) }
                    val navController = rememberNavController()

                    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
                    var selectedItemAllImages by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
                    var selectedItemQuery by remember { mutableStateOf("") }

                    CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                        // Scaffold sem bottomBar — vamos flutuar ela manualmente
                        Scaffold(containerColor = Color.Black) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

                                // Conteúdo ocupa tela toda (sem padding do scaffold)
                                NavHost(
                                    navController = navController,
                                    startDestination = Screen.Search.route,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable(Screen.ForYou.route) {
                                        ForYouScreen(
                                            imageLoader = imageLoader,
                                            onImageClick = { item ->
                                                selectedItem = item
                                                selectedItemAllImages = emptyList()
                                                selectedItemQuery = ""
                                            }
                                        )
                                    }

                                    composable(Screen.Search.route) {
                                        val viewModel: ImageSearchViewModel = koinViewModel()
                                        val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
                                        val currentQuery by viewModel.currentQuery.collectAsState()

                                        ImageSearchScreen(
                                            pagingItems = pagingItems,
                                            onSearch = { viewModel.search(it) },
                                            imageLoader = imageLoader,
                                            hasQuery = !currentQuery.isNullOrBlank(),
                                            onImageClick = { item ->
                                                selectedItem = item
                                                selectedItemAllImages = pagingItems.itemSnapshotList.items
                                                selectedItemQuery = currentQuery ?: ""
                                            }
                                        )
                                    }

                                    composable(Screen.Profile.route) {
                                        ProfileScreen(
                                            imageLoader = imageLoader,
                                            onImageClick = { item ->
                                                selectedItem = item
                                                selectedItemAllImages = emptyList()
                                                selectedItemQuery = ""
                                            }
                                        )
                                    }
                                }

                                // Bottom bar flutuando sobre o conteúdo
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(horizontal = 80.dp, vertical = 20.dp)
                                ) {
                                    BottomNavBar(navController)
                                }

                                // Dialog global
                                selectedItem?.let { item ->
                                    ImageDetailDialog(
                                        item = item,
                                        allImages = selectedItemAllImages,
                                        baseQuery = selectedItemQuery,
                                        onDismiss = { selectedItem = null },
                                        imageLoader = imageLoader
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
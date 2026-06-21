package com.duisternis.voidgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
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
import com.duisternis.voidgrid.ui.screens.ForYouScreen
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

                    // Item selecionado para o Dialog — compartilhado entre telas
                    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
                    var selectedItemAllImages by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
                    var selectedItemQuery by remember { mutableStateOf("") }

                    CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                        Scaffold(
                            containerColor = Color.Black,
                            bottomBar = { BottomNavBar(navController) }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Search.route,
                                modifier = Modifier.padding(innerPadding)
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

                            // Dialog global — funciona em qualquer aba
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
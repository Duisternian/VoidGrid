package com.duisternis.voidgrid.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.ui.components.DominantColorBox
import com.duisternis.voidgrid.ui.viewmodel.FavoritesViewModel
import org.koin.androidx.compose.koinViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Categorias pré-definidas: label visível + valor usado na query do For You
// ─────────────────────────────────────────────────────────────────────────────
data class PresetCategory(val label: String, val value: String)

val PRESET_CATEGORIES = listOf(
    PresetCategory("📖 Mangá",        "manga"),
    PresetCategory("📚 HQ / Comics",  "comic"),
    PresetCategory("🎌 Anime",        "anime"),
    PresetCategory("🎮 Games",        "game"),
    PresetCategory("🖼️ Arte Digital",  "digital art"),
    PresetCategory("✏️ Concept Art",  "concept art"),
    PresetCategory("👾 Pixel Art",    "pixel art"),
    PresetCategory("🌌 Sci-Fi",       "sci-fi"),
    PresetCategory("⚔️ Fantasy",      "fantasy"),
    PresetCategory("🎭 Aesthetic",    "aesthetic"),
    PresetCategory("🌿 Natureza",     "nature"),
    PresetCategory("🌊 Paisagem",     "landscape"),
    PresetCategory("📷 Fotografia",   "photography"),
    PresetCategory("🏗️ Arquitetura",  "architecture"),
    PresetCategory("🎵 Música",       "music"),
    PresetCategory("🚗 Veículos",     "vehicles"),
    PresetCategory("🎲 RPG",          "rpg"),
    PresetCategory("🩸 Horror",       "horror"),
    PresetCategory("🎬 Filmes",       "movie"),
    PresetCategory("📺 Séries",       "series"),
    PresetCategory("🏙️ Urbano",       "urban"),
    PresetCategory("🌸 Kawaii",       "kawaii"),
    PresetCategory("🔮 Mystic",       "mystic art"),
    PresetCategory("⚙️ Steampunk",    "steampunk"),
    PresetCategory("🌃 Cyberpunk",    "cyberpunk"),
)

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    imageLoader: ImageLoader,
    onImageClick: (SearchItem) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }

    // Busca sempre a versão mais atual da pasta (ex: após salvar categorias)
    val currentFolder = selectedFolderId?.let { id -> folders.find { it.id == id } }

    if (currentFolder != null) {
        FolderDetailScreen(
            folder = currentFolder,
            imageLoader = imageLoader,
            onBack = { selectedFolderId = null },
            onImageClick = onImageClick,
            viewModel = viewModel
        )
    } else {
        FolderListScreen(
            folders = folders,
            imageLoader = imageLoader,
            onFolderClick = { selectedFolderId = it.id },
            onDeleteFolder = { viewModel.deleteFolder(it) },
            viewModel = viewModel
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lista de pastas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FolderListScreen(
    folders: List<FolderEntity>,
    imageLoader: ImageLoader,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    viewModel: FavoritesViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Text(
            text = "Pins",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        if (folders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nenhum pin ainda", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        imageLoader = imageLoader,
                        onClick = { onFolderClick(folder) },
                        onDelete = { onDeleteFolder(folder) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card de pasta
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FolderCard(
    folder: FolderEntity,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: FavoritesViewModel
) {
    val coverPin by viewModel.getCoverPin(folder.id).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        if (coverPin != null) {
            DominantColorBox(
                thumbnailUrl = coverPin!!.thumbnail,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverPin!!.link)
                        .crossfade(300)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1C1C1C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Overlay escuro
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // Nome + categorias
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = folder.name,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
            val cats = folder.categoryList()
            if (cats.isNotEmpty()) {
                Text(
                    text = cats.joinToString(" · "),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }

        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Deletar pasta",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = { Text("Deletar pasta?", color = Color.White) },
            text = {
                Text(
                    "Todos os itens em \"${folder.name}\" serão removidos.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Deletar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Detalhe da pasta
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FolderDetailScreen(
    folder: FolderEntity,
    imageLoader: ImageLoader,
    onBack: () -> Unit,
    onImageClick: (SearchItem) -> Unit,
    viewModel: FavoritesViewModel
) {
    val pins by viewModel.getPinsAsSearchItems(folder.id).collectAsState(initial = emptyList())
    var showCategorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        // Categorias
        CategoryRow(
            folder = folder,
            onEditClick = { showCategorySheet = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid de pins
        if (pins.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma imagem nesta pasta", color = Color.Gray)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pins, key = { it.link }) { item ->
                    val aspectRatio = if (item.width > 0 && item.height > 0)
                        item.width.toFloat() / item.height.toFloat()
                    else 0.75f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(item) }
                    ) {
                        DominantColorBox(
                            thumbnailUrl = item.thumbnail,
                            imageLoader = imageLoader,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.encodedLink)
                                    .crossfade(300)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet de categorias
    if (showCategorySheet) {
        CategoryPickerSheet(
            folder = folder,
            sheetState = sheetState,
            onDismiss = { showCategorySheet = false },
            onSave = { updatedFolder ->
                viewModel.updateFolder(updatedFolder)
                showCategorySheet = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Row de categorias no topo da pasta
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryRow(
    folder: FolderEntity,
    onEditClick: () -> Unit
) {
    val cats = folder.categoryList()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            AssistChip(
                onClick = onEditClick,
                label = { Text(if (cats.isEmpty()) "Adicionar categorias" else "Editar") },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF2A2A2A),
                    labelColor = Color.White,
                    leadingIconContentColor = Color.White
                ),
                border = AssistChipDefaults.assistChipBorder(enabled = true)
            )
        }
        items(cats) { cat ->
            SuggestionChip(
                onClick = onEditClick,
                label = { Text(cat, color = Color.White) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1A1A2E)
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(enabled = true)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom sheet — seleção de categorias
// Corrigido: usa LazyColumn para scroll com 25 chips + campo custom + botão
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    folder: FolderEntity,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (FolderEntity) -> Unit
) {
    val selected = remember {
        mutableStateListOf<String>().apply { addAll(folder.categoryList()) }
    }
    var customInput by remember { mutableStateOf("") }
    var showCustomField by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111111),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF444444)) }
    ) {
        // LazyColumn garante scroll quando conteúdo ultrapassa a altura disponível
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Título
            item {
                Text(
                    text = "Categorias",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Usadas para personalizar o For You",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Chips pré-definidos — LazyVerticalGrid dentro de LazyColumn
            // não funciona, então usamos chunked com Row (tamanho controlado
            // pelo weight para não transbordar)
            items(PRESET_CATEGORIES.chunked(3)) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { preset ->
                        val isSelected = selected.contains(preset.value)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selected.remove(preset.value)
                                else selected.add(preset.value)
                            },
                            label = {
                                Text(
                                    preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.weight(1f), // divide espaço igualmente — evita overflow
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1A1A2E),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1C1C1C),
                                labelColor = Color.Gray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = isSelected,
                                enabled = true,
                                selectedBorderColor = Color(0xFF5555AA),
                                borderColor = Color(0xFF333333)
                            )
                        )
                    }
                    // Preenche espaço vazio se a linha tiver menos de 3 itens
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Categorias customizadas já adicionadas
            item {
                val customCats = selected.filter { cat ->
                    PRESET_CATEGORIES.none { it.value == cat }
                }
                if (customCats.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customCats) { cat ->
                            InputChip(
                                selected = true,
                                onClick = { selected.remove(cat) },
                                label = { Text(cat, color = Color.White) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remover",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.Gray
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = Color(0xFF2A1A1A),
                                    selectedLabelColor = Color.White
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    selected = true,
                                    enabled = true,
                                    selectedBorderColor = Color(0xFF884444)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Campo custom (+)
            item {
                if (showCustomField) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            placeholder = { Text("Ex: webtoon, manhwa...", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5555AA),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            )
                        )
                        TextButton(
                            onClick = {
                                val trimmed = customInput.trim().lowercase()
                                if (trimmed.isNotBlank() && !selected.contains(trimmed)) {
                                    selected.add(trimmed)
                                }
                                customInput = ""
                                showCustomField = false
                            }
                        ) {
                            Text("Adicionar", color = Color(0xFF7777CC))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    TextButton(
                        onClick = { showCustomField = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF7777CC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Categoria personalizada", color = Color(0xFF7777CC))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Botão salvar
            item {
                Button(
                    onClick = {
                        onSave(folder.copy(categories = selected.joinToString(",")))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E))
                ) {
                    Text("Salvar", color = Color.White)
                }
            }
        }
    }
}
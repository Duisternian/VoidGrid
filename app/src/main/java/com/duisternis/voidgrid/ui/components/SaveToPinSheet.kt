package com.duisternis.voidgrid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.ui.viewmodel.FavoritesViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToPinSheet(
    item: SearchItem,
    onDismiss: () -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1C),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(
                text = "Salvar em",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Opção de nova pasta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNewFolderDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Nova pasta", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            LazyColumn {
                items(folders) { folder ->
                    FolderRow(folder = folder, onClick = {
                        viewModel.pinItem(item, folder.id)
                        onDismiss()
                    })
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = { Text("Nova pasta", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Nome da pasta", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName) { folderId ->
                            viewModel.pinItem(item, folderId)
                        }
                        showNewFolderDialog = false
                        onDismiss()
                    }
                }) { Text("Criar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun FolderRow(folder: FolderEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(folder.name, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}
package com.duisternis.voidgrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.duisternis.voidgrid.data.model.SearchProvider

private val PanelBg = Color(0xFF191919)
private val ItemSelectedBg = Color(0xFF2A2A2A)
private val DividerColor = Color.White.copy(alpha = 0.08f)
private val TextSecondary = Color.White.copy(alpha = 0.5f)

@Composable
fun ProviderDropdown(
    expanded: Boolean,
    selectedProvider: SearchProvider,
    customDomains: List<String>,
    safeSearch: Boolean,
    onSelect: (SearchProvider) -> Unit,
    onAddCustom: (String) -> Unit,
    onRemoveCustom: (String) -> Unit,
    onToggleSafeSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    var addingCustom by remember(expanded) { mutableStateOf(false) }
    var customText by remember(expanded) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = PanelBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.widthIn(min = 220.dp, max = 280.dp)
    ) {
        Text(
            text = "Buscar em",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
        )

        SearchProvider.builtIn.forEach { provider ->
            ProviderRow(
                label = provider.label,
                selected = selectedProvider.id == provider.id,
                onClick = {
                    onSelect(provider)
                    onDismiss()
                }
            )
        }

        if (customDomains.isNotEmpty()) {
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 6.dp))
            Text(
                text = "Salvos",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
            customDomains.forEach { domain ->
                ProviderRow(
                    label = domain,
                    selected = selectedProvider.id == "custom:$domain",
                    onClick = {
                        onSelect(SearchProvider.Custom(domain))
                        onDismiss()
                    },
                    onRemove = { onRemoveCustom(domain) }
                )
            }
        }

        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 6.dp))

        if (addingCustom) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val confirm = {
                    if (customText.isNotBlank()) {
                        onAddCustom(customText)
                        focusManager.clearFocus()
                        onDismiss()
                    }
                }
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("site.com", color = TextSecondary) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF111111),
                        unfocusedContainerColor = Color(0xFF111111),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = confirm) {
                    Icon(Icons.Default.Check, contentDescription = "Confirmar", tint = Color.White)
                }
            }
        } else {
            ProviderRow(
                label = "Custom",
                icon = Icons.Default.Add,
                selected = false,
                onClick = { addingCustom = true }
            )
        }

        // Divisor + toggle SafeSearch
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleSafeSearch() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SafeSearch",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (safeSearch) "Conteúdo explícito oculto" else "Mostrando todo conteúdo",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Switch(
                checked = safeSearch,
                onCheckedChange = { onToggleSafeSearch() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF555555)
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun ProviderRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ItemSelectedBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remover", tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
    }
}
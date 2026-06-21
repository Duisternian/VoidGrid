package com.duisternis.voidgrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String) {
    object ForYou : Screen("for_you")
    object Search : Screen("search")
    object Profile : Screen("profile")
}

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(Screen.ForYou, Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem(Screen.Search, Icons.Filled.Search, Icons.Outlined.Search),
        BottomNavItem(Screen.Profile, Icons.Filled.Folder, Icons.Outlined.FolderOpen)
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF191919))
            .height(50.dp)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp) // tamanho do item clicável
                    .clip(CircleShape) // indicador sempre circular
                    .background(
                        if (selected) Color.White.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                    .clickable {
                        if (currentRoute != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Search.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = null,
                    tint = if (selected) Color.White else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
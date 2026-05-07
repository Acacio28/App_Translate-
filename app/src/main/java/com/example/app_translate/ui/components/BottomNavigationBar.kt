package com.example.app_translate.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.ui.theme.WhiteColor
import com.example.app_translate.ui.theme.LightPurpleColor

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onItemSelected: (String) -> Unit
) {
    val items = listOf(
        NavigationItem("Translate", Icons.Default.Translate, "translate"),
        NavigationItem("Dialogue", Icons.Default.Chat, "dialogue"),
        NavigationItem("History", Icons.Default.History, "history"),
        NavigationItem("Settings", Icons.Default.Settings, "settings")
    )

    NavigationBar(
        containerColor = WhiteColor,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = { onItemSelected(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PurpleColor,
                    selectedTextColor = PurpleColor,
                    indicatorColor = LightPurpleColor,
                    unselectedIconColor = PurpleColor.copy(alpha = 0.5f),
                    unselectedTextColor = PurpleColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}

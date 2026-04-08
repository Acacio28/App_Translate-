package com.example.app_translate.ui.components
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.ui.theme.WhiteColor

// Data class untuk item menu
data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationBar() {
    val items = listOf(
        NavigationItem("Translate", Icons.Default.Translate, "translate"),
        NavigationItem("History", Icons.Default.History, "history"),
        NavigationItem("Settings", Icons.Default.Settings, "settings")
    )

    NavigationBar(
        containerColor = WhiteColor,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = item.route == "translate", // Sementara kita set translate yang aktif
                onClick = { /* Nanti tambahkan navigasi di sini */ },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PurpleColor,
                    selectedTextColor = PurpleColor,
                    indicatorColor = WhiteColor // Menghilangkan background lonjong saat dipilih
                )
            )
        }
    }
}

package com.example.app_translate.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.CameraAlt // New import
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

// Import warna dari theme kamu
data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationBar(currentRoute: String, onItemSelected: (String) -> Unit) { // Added parameters
    val items = listOf(
        NavigationItem("Translate", Icons.Default.Translate, "translate"),
        NavigationItem("Camera", Icons.Default.CameraAlt, "camera"), // Added Camera item
        NavigationItem("History", Icons.Default.History, "history"),
        NavigationItem("Settings", Icons.Default.Settings, "settings")
    )

    NavigationBar(
        containerColor = WhiteColor, // Sekarang tidak akan ambigu lagi
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route, // Corrected selected logic
                onClick = { onItemSelected(item.route) }, // Corrected onClick logic
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PurpleColor,
                    selectedTextColor = PurpleColor,
                    indicatorColor = WhiteColor
                )
            )
        }
    }
}
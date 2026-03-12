package com.example.flexfi.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.flexfi.ui.theme.FlexFiBlue
import com.example.flexfi.ui.theme.FlexFiLightText

enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    GROUPS("Groups", Icons.Filled.Groups, Icons.Outlined.Groups),
    CONTACTS("Contacts", Icons.Filled.Person, Icons.Outlined.Person),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun FlexFiBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = selected,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FlexFiBlue,
                    selectedTextColor = FlexFiBlue,
                    unselectedIconColor = FlexFiLightText,
                    unselectedTextColor = FlexFiLightText,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

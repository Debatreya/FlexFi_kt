package com.example.flexfi.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupViewModel,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onHomeTab: () -> Unit,
    onContactsTab: () -> Unit,
    onProfileTab: () -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(BottomNavTab.GROUPS) }

    LaunchedEffect(Unit) {
        viewModel.refreshGroups()
    }

    val filtered = if (searchQuery.isBlank()) groups
                   else groups.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Groups",
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "Search", tint = FlexFiDarkText)
                    }
                }
            )
        },
        bottomBar = {
            FlexFiBottomNavBar(
                currentTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        BottomNavTab.HOME -> onHomeTab()
                        BottomNavTab.CONTACTS -> onContactsTab()
                        BottomNavTab.PROFILE -> onProfileTab()
                        else -> {}
                    }
                }
            )
        },
        floatingActionButton = {
            FlexFiFab(onClick = onCreateGroupClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (showSearch) {
                item {
                    FlexFiSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search groups..."
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No groups yet.\nTap + to create your first group!",
                            color = FlexFiBodyText,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filtered) { group ->
                    // Group emoji based on name hash
                    val emojis = listOf("🏠", "✈️", "🎮", "🍕", "🎉", "💰", "🎬", "☕")
                    val emoji = emojis[kotlin.math.abs(group.name.hashCode()) % emojis.size]

                    FlexFiGroupCard(
                        name = group.name,
                        memberCount = 0, // Will be enriched with real data
                        totalBalance = CurrencyProvider.formatAmount(group.totalExpense),
                        balanceColor = if (group.totalExpense >= 0) FlexFiGreen else FlexFiRed,
                        latestActivity = "Tap to view",
                        groupIcon = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FlexFiLightBlue,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        },
                        memberAvatars = {
                            // Placeholder avatars
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy((-8).dp)
                            ) {
                                items(3) { i ->
                                    FlexFiAvatar(
                                        name = "M${i + 1}",
                                        size = AvatarSize.SMALL
                                    )
                                }
                            }
                        },
                        onClick = { onGroupClick(group.id) }
                    )
                }
            }
        }
    }
}

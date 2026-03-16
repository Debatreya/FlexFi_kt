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
import com.example.flexfi.data.local.dao.GroupMemberInfo
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
    onExpensesTab: () -> Unit,
    onProfileTab: () -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(BottomNavTab.GROUPS) }

    // Cache member data per group
    var memberCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var memberInfos by remember { mutableStateOf<Map<String, List<GroupMemberInfo>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        viewModel.refreshGroups()
    }

    // Load member data when groups change
    LaunchedEffect(groups) {
        val counts = mutableMapOf<String, Int>()
        val infos = mutableMapOf<String, List<GroupMemberInfo>>()
        for (group in groups) {
            counts[group.id] = viewModel.getMemberCount(group.id)
            infos[group.id] = viewModel.getGroupMembersOnce(group.id)
        }
        memberCounts = counts
        memberInfos = infos
    }

    val filtered = if (searchQuery.isBlank()) groups
                   else groups.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "Groups",
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onSurface)
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
                        BottomNavTab.EXPENSES -> onExpensesTab()
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
                    val totalExpense by viewModel.getGroupTotalExpense(group.id).collectAsState(initial = 0.0)

                    // Group emoji based on name hash
                    val emojis = listOf("🏠", "✈️", "🎮", "🍕", "🎉", "💰", "🎬", "☕")
                    val emoji = emojis[kotlin.math.abs(group.name.hashCode()) % emojis.size]

                    val count = memberCounts[group.id] ?: 0
                    val members = memberInfos[group.id] ?: emptyList()

                    FlexFiGroupCard(
                        name = group.name,
                        memberCount = count,
                        totalBalance = CurrencyProvider.formatAmount(totalExpense),
                        balanceColor = if (totalExpense >= 0) FlexFiGreen else FlexFiRed,
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
                            if (members.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy((-8).dp)
                                ) {
                                    items(members) { member ->
                                        FlexFiAvatar(
                                            name = member.contactName ?: member.phone.takeLast(4),
                                            size = AvatarSize.SMALL,
                                            isGhost = member.isGhost == true
                                        )
                                    }
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

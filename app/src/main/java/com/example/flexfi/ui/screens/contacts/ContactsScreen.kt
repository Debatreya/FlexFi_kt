package com.example.flexfi.ui.screens.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    onDeleteContact: (String) -> Unit,
    onAddContactClick: () -> Unit,
    onBackClick: () -> Unit,
    onContactsTab: () -> Unit,
    onGroupsTab: () -> Unit,
    onHomeTab: () -> Unit,
    onProfileTab: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(BottomNavTab.CONTACTS) }

    val filtered = if (searchQuery.isBlank()) contacts
                   else contacts.filter {
                       it.name.contains(searchQuery, ignoreCase = true) ||
                       it.phone.contains(searchQuery)
                   }
    val flexFiContacts = filtered.filter { it.isGhost == false }
    val ghostContacts = filtered.filter { it.isGhost == true }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Contacts",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, "More", tint = FlexFiDarkText)
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
                        BottomNavTab.GROUPS -> onGroupsTab()
                        BottomNavTab.CONTACTS -> onContactsTab()
                        BottomNavTab.PROFILE -> onProfileTab()
                    }
                }
            )
        },
        floatingActionButton = {
            FlexFiFab(onClick = onAddContactClick)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Search
            item {
                FlexFiSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search contacts..."
                )
            }

            // ON FLEXFI section
            if (flexFiContacts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(
                            "ON FLEXFI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlexFiBodyText,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FlexFiGreenLight
                        ) {
                            Text(
                                "${flexFiContacts.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlexFiGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                items(flexFiContacts) { contact ->
                    FlexFiContactRow(
                        name = contact.name,
                        phone = contact.phone,
                        showOnlineDot = true,
                        trailingContent = {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Synced",
                                tint = FlexFiGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            // INVITE section
            if (ghostContacts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "INVITE TO FLEXFI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiBodyText,
                        letterSpacing = 1.sp
                    )
                }
                items(ghostContacts) { contact ->
                    FlexFiContactRow(
                        name = contact.name,
                        phone = contact.phone,
                        isGhost = true,
                        trailingContent = {
                            FilledTonalButton(
                                onClick = { /* invite */ },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = FlexFiLightBlue,
                                    contentColor = FlexFiBlue
                                )
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Invite", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }
        }
    }
}

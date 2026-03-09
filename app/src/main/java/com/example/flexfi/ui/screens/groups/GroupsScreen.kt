package com.example.flexfi.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupViewModel,
    onCreateGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit
) {
    val groups by viewModel.groups.collectAsState()

    // Refresh groups every time this screen is entered
    // This ensures data shows immediately after login (not just on ViewModel init)
    LaunchedEffect(Unit) {
        viewModel.refreshGroups()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Groups") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGroupClick) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No groups yet")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(groups) { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        leadingContent = { Icon(Icons.Default.Groups, contentDescription = null) },
                        modifier = Modifier.clickable { onGroupClick(group.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

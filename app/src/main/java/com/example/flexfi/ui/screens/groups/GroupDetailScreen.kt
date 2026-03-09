package com.example.flexfi.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flexfi.data.local.entities.GroupEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupViewModel,
    onEditClick: (String) -> Unit,
    onDeleteSuccess: () -> Unit
) {
    var group by remember { mutableStateOf<GroupEntity?>(null) }
    val members by viewModel.getGroupMembers(groupId).collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    val currentUserPhone = viewModel.currentUserPhone

    LaunchedEffect(groupId) {
        group = viewModel.getGroupById(groupId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete this group? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(groupId)
                    onDeleteSuccess()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Loading...") },
                actions = {
                    // Only Admin can edit
                    if (group?.adminPhone == currentUserPhone) {
                        IconButton(onClick = { onEditClick(groupId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Group")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Group")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            group?.let {
                val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it.createdAt))
                Text(
                    text = "Created on $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Text("Members", style = MaterialTheme.typography.titleLarge)
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(members) { member ->
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isMe = member.phone == currentUserPhone
                                val displayName = when {
                                    isMe -> "Me"
                                    member.contactName != null -> member.contactName
                                    else -> member.phone
                                }
                                Text(displayName)
                                
                                if (member.phone == group?.adminPhone) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("Admin", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        supportingContent = { 
                            if (member.phone != currentUserPhone) {
                                Text(member.phone) 
                            }
                        },
                        trailingContent = {
                            if (member.isGhost == true) {
                                Text("👻 Ghost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            } else if (member.isGhost == false) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "FlexFi User", tint = Color.Green)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

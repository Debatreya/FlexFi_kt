package com.example.flexfi.ui.screens.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactViewModel,
    onAddContactClick: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    var contactToDelete by remember { mutableStateOf<String?>(null) }

    // Refresh contacts (ghost → registered sync) every time this screen is entered
    LaunchedEffect(Unit) {
        viewModel.refreshContacts()
    }

    // Delete confirmation dialog
    contactToDelete?.let { contactId ->
        val contact = contacts.find { it.id == contactId }
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete ${contact?.name ?: "this contact"}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContact(contactId)
                    contactToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Contacts") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddContactClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No contacts yet")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(contacts) { contact ->
                    ListItem(
                        headlineContent = { Text(contact.name) },
                        supportingContent = { Text(contact.phone) },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (contact.isGhost) {
                                    Text("👻 Ghost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "FlexFi User", tint = Color.Green)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { contactToDelete = contact.id }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Contact",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

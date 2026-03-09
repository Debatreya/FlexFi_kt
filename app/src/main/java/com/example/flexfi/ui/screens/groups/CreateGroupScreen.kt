package com.example.flexfi.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flexfi.ui.screens.contacts.ContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    groupViewModel: GroupViewModel,
    contactViewModel: ContactViewModel,
    onGroupCreated: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val contacts by contactViewModel.contacts.collectAsState()
    val selectedMembers = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create Group") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Select Members (Optional)", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(contacts) { contact ->
                    val isSelected = selectedMembers.contains(contact.phone)
                    ListItem(
                        headlineContent = { Text(contact.name) },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingContent = {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable {
                            if (isSelected) selectedMembers.remove(contact.phone)
                            else selectedMembers.add(contact.phone)
                        }
                    )
                    HorizontalDivider()
                }
            }

            Button(
                onClick = {
                    val selectedEntities = contacts.filter { selectedMembers.contains(it.phone) }
                    groupViewModel.createGroup(groupName, selectedEntities)
                    onGroupCreated()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupName.isNotBlank()
            ) {
                Text("Create Group")
            }
        }
    }
}

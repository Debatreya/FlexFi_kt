package com.example.flexfi.ui.screens.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddContactScreen(
    viewModel: ContactViewModel,
    onContactAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add New Contact", style = MaterialTheme.typography.headlineMedium)
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { 
                phone = it
                phoneError = null
            },
            label = { Text("Phone Number (with country code)") },
            modifier = Modifier.fillMaxWidth(),
            isError = phoneError != null,
            supportingText = phoneError?.let { { Text(it) } }
        )

        Text(
            text = "If a contact with this phone number already exists, its name will be updated.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Phone validation
                val trimmedPhone = phone.trim()
                when {
                    !trimmedPhone.startsWith("+") -> {
                        phoneError = "Phone number must start with country code (e.g. +91)"
                    }
                    trimmedPhone.length < 10 -> {
                        phoneError = "Phone number is too short"
                    }
                    else -> {
                        viewModel.addContact(name.trim(), trimmedPhone)
                        onContactAdded()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && phone.isNotBlank()
        ) {
            Text("Save Contact")
        }
    }
}

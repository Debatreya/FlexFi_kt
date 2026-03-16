package com.example.flexfi.ui.screens.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*

@Composable
fun AddContactScreen(
    viewModel: ContactViewModel,
    onContactAdded: () -> Unit,
    onBack: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Add Contact",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("CONTACT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlexFiBodyText, letterSpacing = 1.sp)

                    FlexFiTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Name",
                        placeholder = "Contact's name",
                        leadingIcon = Icons.Default.Person
                    )

                    FlexFiTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                        },
                        label = "Phone Number (with country code)",
                        placeholder = "+91XXXXXXXXXX",
                        leadingIcon = Icons.Default.Phone,
                        isError = phoneError != null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )

                    if (phoneError != null) {
                        Text(
                            text = phoneError!!,
                            color = FlexFiRed,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "If a contact with this phone number already exists, its name will be updated.",
                        fontSize = 12.sp,
                        color = FlexFiLightText
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            FlexFiPrimaryButton(
                text = "Save Contact",
                onClick = {
                    val trimmedPhone = phone.trim()
                    when {
                        !trimmedPhone.startsWith("+") -> {
                            phoneError = "Phone number must start with country code (e.g. +91)"
                        }
                        trimmedPhone.length < 10 -> {
                            phoneError = "Phone number is too short"
                        }
                        else -> {
                            viewModel.addContact(name.trim(), trimmedPhone) {
                                onContactAdded()
                            }
                        }
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                trailingIcon = {
                    Icon(Icons.Default.Check, null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

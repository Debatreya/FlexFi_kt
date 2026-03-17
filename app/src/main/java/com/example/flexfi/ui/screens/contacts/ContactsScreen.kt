package com.example.flexfi.ui.screens.contacts

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.data.local.entities.ContactEntity
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider

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
    onExpensesTab: () -> Unit,
    onProfileTab: () -> Unit,
    onSendMoney: (ContactEntity, Double, String, String, () -> Unit, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(BottomNavTab.CONTACTS) }
    
    var showSendMoneyDialog by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<ContactEntity?>(null) }
    var sendAmount by remember { mutableStateOf("") }
    var sendCurrency by remember { mutableStateOf(CurrencyProvider.displayCurrencyCode) }
    var sendNote by remember { mutableStateOf("Direct Transfer") }
    var currencyExpanded by remember { mutableStateOf(false) }

    if (showSendMoneyDialog && selectedContact != null) {
        AlertDialog(
            onDismissRequest = { showSendMoneyDialog = false },
            title = { Text("Send Money") },
            text = {
                Column {
                    Text(
                        "How much did you send ${selectedContact!!.name}?",
                        fontSize = 14.sp,
                        color = FlexFiBodyText
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sendAmount,
                        onValueChange = { sendAmount = it },
                        label = { Text("Amount Sent") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        prefix = { Text(sendCurrency) }
                    )
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded }
                    ) {
                        OutlinedTextField(
                            value = sendCurrency,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            CurrencyProvider.supportedCurrencyCodes.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code) },
                                    onClick = {
                                        sendCurrency = code
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sendNote,
                        onValueChange = { sendNote = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Note: Actual payment is done outside the app. This will record a payment so they owe you less/you owe them more.",
                        fontSize = 11.sp,
                        color = FlexFiLightText
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = sendAmount.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        Toast.makeText(context, "Enter valid amount", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    onSendMoney(
                        selectedContact!!,
                        amount,
                        sendCurrency,
                        sendNote,
                        {
                            showSendMoneyDialog = false
                            sendAmount = ""
                            Toast.makeText(context, "Payment recorded!", Toast.LENGTH_SHORT).show()
                        },
                        {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    )
                }) {
                    Text("Send", color = FlexFiBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendMoneyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val filtered = if (searchQuery.isBlank()) contacts
                   else contacts.filter {
                       it.name.contains(searchQuery, ignoreCase = true) ||
                       it.phone.contains(searchQuery)
                   }
    val flexFiContacts = filtered.filter { it.isGhost == false }
    val ghostContacts = filtered.filter { it.isGhost == true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "Contacts",
                showBackButton = true,
                onBackClick = onBackClick
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
                        BottomNavTab.EXPENSES -> onExpensesTab()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        selectedContact = contact
                                        showSendMoneyDialog = true
                                        sendAmount = ""
                                        sendNote = "Direct Transfer"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Send", fontSize = 12.sp, color = FlexFiBlue)
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Synced",
                                    tint = FlexFiGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

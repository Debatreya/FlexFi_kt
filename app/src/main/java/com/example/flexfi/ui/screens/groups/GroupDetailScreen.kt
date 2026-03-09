package com.example.flexfi.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.ui.screens.expenses.ExpenseListItem
import com.example.flexfi.ui.screens.expenses.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupViewModel,
    expenseViewModel: ExpenseViewModel,
    onEditClick: (String) -> Unit,
    onDeleteSuccess: () -> Unit,
    onAddExpenseClick: (String) -> Unit
) {
    var group by remember { mutableStateOf<GroupEntity?>(null) }
    val members by viewModel.getGroupMembers(groupId).collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    val currentUserPhone = viewModel.currentUserPhone

    val expenses by expenseViewModel.expenses.collectAsState()
    val balances by expenseViewModel.balances.collectAsState()
    val settlements by expenseViewModel.settlements.collectAsState()

    LaunchedEffect(groupId) {
        group = viewModel.getGroupById(groupId)
        expenseViewModel.loadExpensesForGroup(groupId)
    }

    // Helper map to show names instead of raw phone numbers
    val memberNames = remember(members, currentUserPhone) {
        members.associate { 
            it.phone to if (it.phone == currentUserPhone) "Me" else it.contactName ?: it.phone 
        }
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddExpenseClick(groupId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                group?.let {
                    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it.createdAt))
                    Text(
                        text = "Created on $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Balances Section
            if (balances.isNotEmpty()) {
                item {
                    Text("Balances", style = MaterialTheme.typography.titleLarge)
                }
                items(balances.toList().sortedByDescending { it.second }) { (phone, balance) ->
                    val displayName = memberNames[phone] ?: phone
                    val color = if (balance > 0) Color(0xFF4CAF50) else if (balance < 0) Color(0xFFE53935) else Color.Gray
                    val text = when {
                        balance > 0 -> "gets back ₹${"%.2f".format(balance)}"
                        balance < 0 -> "owes ₹${"%.2f".format(-balance)}"
                        else -> "is settled up"
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(displayName, fontWeight = FontWeight.Medium)
                        Text(text, color = color, fontWeight = FontWeight.Bold)
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Settlements Section
            if (settlements.isNotEmpty()) {
                item {
                    Text("Suggested Settlements", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                items(settlements) { settlement ->
                    val fromName = memberNames[settlement.fromPhone] ?: settlement.fromPhone
                    val toName = memberNames[settlement.toPhone] ?: settlement.toPhone
                    
                    val settlementText = if (settlement.fromPhone == currentUserPhone) {
                        "You owe $toName ₹${"%.2f".format(settlement.amount)}"
                    } else if (settlement.toPhone == currentUserPhone) {
                        "$fromName owes You ₹${"%.2f".format(settlement.amount)}"
                    } else {
                        "$fromName owes $toName ₹${"%.2f".format(settlement.amount)}"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = settlementText,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Expenses Section
            if (expenses.isNotEmpty()) {
                item {
                    Text("Expenses", style = MaterialTheme.typography.titleLarge)
                }
                items(expenses) { expense ->
                    ExpenseListItem(
                        expense = expense,
                        currentUserPhone = currentUserPhone,
                        memberNames = memberNames
                    )
                    HorizontalDivider()
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Members Section
            item {
                Text("Members", style = MaterialTheme.typography.titleLarge)
            }
            items(members) { member ->
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(memberNames[member.phone] ?: member.phone)
                            
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
            }
        }
    }
}

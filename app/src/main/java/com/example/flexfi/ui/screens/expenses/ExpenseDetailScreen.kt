package com.example.flexfi.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.data.local.entities.ExpenseEntity
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.screens.groups.GroupViewModel
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    viewModel: ExpenseViewModel,
    groupViewModel: GroupViewModel,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val expense = expenses.find { it.id == expenseId }
    val splits by viewModel.currentSplits.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) {
        viewModel.loadSplitsForExpense(expenseId)
    }

    if (expense == null) {
        // Handle gracefully if deleted or missing
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val members by groupViewModel.getGroupMembers(expense.groupId).collectAsState(initial = emptyList())
    val currentUserPhone = groupViewModel.currentUserPhone

    val memberNames = remember(members, currentUserPhone) {
        members.associate {
            it.phone to if (it.phone == currentUserPhone) "Me" else it.contactName ?: it.phone
        }
    }

    val paidByName = memberNames[expense.paidByPhone] ?: expense.paidByPhone
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure? This will remove the expense and update all group balances.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(expenseId)
                    onDeleteSuccess()
                }) { Text("Delete", color = FlexFiRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Expense Details",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    if (expense.paidByPhone == currentUserPhone || groupViewModel.currentUserPhone.isNotBlank()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = FlexFiRed)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Expense Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FlexFiBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = FlexFiWhite.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val icon = when (expense.category.lowercase()) {
                                    "food" -> Icons.Default.Restaurant
                                    "transport" -> Icons.Default.DirectionsCar
                                    "shopping" -> Icons.Default.ShoppingBag
                                    "entertainment" -> Icons.Default.SportsEsports
                                    "health" -> Icons.Default.MedicalServices
                                    "utilities" -> Icons.Default.Bolt
                                    "rent" -> Icons.Default.Home
                                    else -> Icons.Default.Receipt
                                }
                                Icon(icon, null, tint = FlexFiWhite, modifier = Modifier.size(32.dp))
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Text(
                            expense.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlexFiWhite
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            CurrencyProvider.formatAmount(expense.baseAmount),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FlexFiWhite
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FlexFiWhite.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Paid by $paidByName on ${dateFormat.format(Date(expense.createdAt))}",
                                fontSize = 12.sp,
                                color = FlexFiWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Split Details Section
            if (splits.isNotEmpty()) {
                item {
                    Text("How it was split", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                items(splits) { split ->
                    val isPayer = split.memberPhone == expense.paidByPhone
                    val memberName = memberNames[split.memberPhone] ?: split.memberPhone
                    val shareFormatted = CurrencyProvider.formatAmount(split.shareAmount)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlexFiAvatar(name = memberName, size = AvatarSize.SMALL)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    memberName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (isPayer) "Paid and owes $shareFormatted" else "Owes $shareFormatted",
                                    fontSize = 13.sp,
                                    color = if (isPayer) FlexFiBlue else FlexFiOrange
                                )
                            }
                            Text(
                                shareFormatted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

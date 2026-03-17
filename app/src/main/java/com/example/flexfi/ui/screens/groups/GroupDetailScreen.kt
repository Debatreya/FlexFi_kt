package com.example.flexfi.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.flexfi.data.local.entities.GroupEntity
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.screens.expenses.ExpenseViewModel
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
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
    onAddExpenseClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onSettleUpClick: () -> Unit
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

    val memberNames = remember(members, currentUserPhone) {
        members.associate {
            it.phone to if (it.phone == currentUserPhone) "Me" else it.contactName ?: it.phone
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(groupId)
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
                title = group?.name ?: "Loading...",
                showBackButton = true,
                onBackClick = onDeleteSuccess,
                actions = {
                    if (group?.adminPhone == currentUserPhone) {
                        IconButton(onClick = { onEditClick(groupId) }) {
                            Icon(Icons.Default.Edit, "Edit", tint = FlexFiDarkText)
                        }
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
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Member avatars row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members) { member ->
                        val name = memberNames[member.phone] ?: member.phone
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FlexFiAvatar(
                                name = name,
                                size = AvatarSize.MEDIUM,
                                isGhost = member.isGhost == true,
                                showOnlineDot = member.isGhost == false
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                name.take(8),
                                fontSize = 11.sp,
                                color = FlexFiBodyText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Balance card
            if (balances.isNotEmpty()) {
                item {
                    val myBalance = balances[currentUserPhone] ?: 0.0
                    FlexFiGradientCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("YOUR BALANCE", fontSize = 10.sp, color = FlexFiWhite.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        myBalance > 0 -> "You are owed"
                                        myBalance < 0 -> "You owe"
                                        else -> "Settled up!"
                                    },
                                    fontSize = 13.sp,
                                    color = FlexFiWhite.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = CurrencyProvider.formatAmount(kotlin.math.abs(myBalance)),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlexFiWhite
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("GROUP TOTAL", fontSize = 10.sp, color = FlexFiWhite.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = CurrencyProvider.formatAmount(expenses.sumOf { it.baseAmount }),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlexFiWhite
                                )
                            }
                        }
                    }
                }
            }

            // Settle Up button
            if (balances.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = onSettleUpClick,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Handshake, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Settle Up", fontSize = 14.sp)
                    }
                }
            }

            // Settlements
            if (settlements.isNotEmpty()) {
                item {
                    Text("Suggested Settlements", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
                }
                items(settlements) { settlement ->
                    val fromName = memberNames[settlement.fromPhone] ?: settlement.fromPhone
                    val toName = memberNames[settlement.toPhone] ?: settlement.toPhone
                    val text = when {
                        settlement.fromPhone == currentUserPhone -> "You owe $toName"
                        settlement.toPhone == currentUserPhone -> "$fromName owes You"
                        else -> "$fromName → $toName"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FlexFiWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SwapHoriz, null, tint = FlexFiBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(text, modifier = Modifier.weight(1f), fontSize = 14.sp, color = FlexFiDarkText)
                            Text(
                                CurrencyProvider.formatAmount(settlement.amount),
                                fontWeight = FontWeight.Bold,
                                color = FlexFiBlue,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Expenses
            if (expenses.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Expense History", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
                }
                items(expenses) { expense ->
                    val paidByName = memberNames[expense.paidByPhone] ?: expense.paidByPhone
                    val icon = when (expense.category.lowercase()) {
                        "food" -> Icons.Default.Restaurant
                        "transport" -> Icons.Default.DirectionsCar
                        "shopping" -> Icons.Default.ShoppingBag
                        "entertainment" -> Icons.Default.SportsEsports
                        else -> Icons.Default.Receipt
                    }

                    FlexFiExpenseCard(
                        title = expense.title,
                        subtitle = "Paid by $paidByName • ${expense.category}",
                        amount = CurrencyProvider.formatAmount(expense.baseAmount),
                        statusText = "SPLIT",
                        statusColor = FlexFiBlue,
                        icon = icon,
                        iconBgColor = FlexFiLightBlue,
                        iconTint = FlexFiBlue,
                        onClick = { onExpenseClick(expense.id) }
                    )
                }
            }

            // Add expense button
            item {
                Spacer(Modifier.height(8.dp))
                FlexFiPrimaryButton(
                    text = "Add Expense",
                    onClick = { onAddExpenseClick(groupId) },
                    trailingIcon = {
                        Icon(Icons.Default.Add, null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}

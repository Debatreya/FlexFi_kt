package com.example.flexfi.ui.screens.personal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexfi.data.local.entities.PersonalExpenseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDashboardScreen(
    viewModel: PersonalExpenseViewModel,
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (PersonalExpenseEntity) -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()

    // Category totals derived from expense list
    val categoryTotals: Map<String, Double> = remember(expenses) {
        expenses
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 Your Spending") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpenseClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── Total Spent Card ──────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "₹${"%.2f".format(totalSpent)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ─── Category Breakdown ────────────────────────────
            if (categoryTotals.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            categoryTotals.entries.forEachIndexed { index, (category, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "₹${"%.2f".format(amount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (index < categoryTotals.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ─── Recent Expenses ────────────────────────────────
            if (expenses.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(expenses) { expense ->
                    PersonalExpenseItem(
                        expense = expense,
                        onClick = { onExpenseClick(expense) }
                    )
                }
                // Bottom padding so FAB doesn't overlap last item
                item { Spacer(Modifier.height(80.dp)) }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses yet.\nTap + to add your first expense!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalExpenseItem(
    expense: PersonalExpenseEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description ?: "Expense",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Source badge
                    val badgeLabel = if (expense.source == "GROUP") "Group" else "Personal"
                    val badgeColor = if (expense.source == "GROUP")
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.secondary
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = "₹${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

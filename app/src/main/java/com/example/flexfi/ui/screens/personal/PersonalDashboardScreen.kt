package com.example.flexfi.ui.screens.personal

import androidx.compose.foundation.clickable
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
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider

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

    val categoryTotals = remember(expenses) {
        expenses
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }

    val maxCategory = categoryTotals.values.maxOrNull() ?: 1.0

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Your Spending",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, "Notifications", tint = FlexFiDarkText)
                    }
                }
            )
        },
        floatingActionButton = {
            FlexFiFab(onClick = onAddExpenseClick)
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

            // Total Spent card
            item {
                FlexFiGradientCard {
                    Text("MONTHLY SPENDING", fontSize = 11.sp, color = FlexFiWhite.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CurrencyProvider.formatAmount(totalSpent),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlexFiWhite
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FlexFiWhite.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "This Month",
                                fontSize = 11.sp,
                                color = FlexFiWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Category Breakdown
            if (categoryTotals.isNotEmpty()) {
                item {
                    Text("Category Breakdown", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = FlexFiWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            categoryTotals.entries.forEach { (cat, amount) ->
                                val icon = when (cat.lowercase()) {
                                    "food" -> Icons.Default.Restaurant
                                    "transport" -> Icons.Default.DirectionsCar
                                    "shopping" -> Icons.Default.ShoppingBag
                                    "entertainment" -> Icons.Default.SportsEsports
                                    "health" -> Icons.Default.LocalHospital
                                    "utilities" -> Icons.Default.Bolt
                                    "rent" -> Icons.Default.Home
                                    else -> Icons.Default.MoreHoriz
                                }
                                val color = when (cat.lowercase()) {
                                    "food" -> CategoryFood
                                    "transport" -> CategoryTransport
                                    "shopping" -> CategoryShopping
                                    "entertainment" -> CategoryEntertainment
                                    "health" -> CategoryHealth
                                    "utilities" -> CategoryUtilities
                                    "rent" -> CategoryRent
                                    else -> CategoryOther
                                }
                                FlexFiCategoryProgressBar(
                                    icon = icon,
                                    iconTint = color,
                                    label = cat,
                                    amount = CurrencyProvider.formatAmount(amount),
                                    progress = (amount / maxCategory).toFloat(),
                                    progressColor = color
                                )
                            }
                        }
                    }
                }
            }

            // Recent Activity
            if (expenses.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Activity", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
                        Row {
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.FilterList, null, tint = FlexFiBodyText, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Search, null, tint = FlexFiBodyText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                items(expenses) { expense ->
                    val icon = when (expense.category.lowercase()) {
                        "food" -> Icons.Default.Restaurant
                        "transport" -> Icons.Default.DirectionsCar
                        "shopping" -> Icons.Default.ShoppingBag
                        else -> Icons.Default.Receipt
                    }
                    val iconColor = when (expense.category.lowercase()) {
                        "food" -> CategoryFood
                        "transport" -> CategoryTransport
                        "shopping" -> CategoryShopping
                        else -> CategoryOther
                    }
                    FlexFiExpenseCard(
                        title = expense.description ?: "Expense",
                        subtitle = "${expense.category} • ${if (expense.source == "GROUP") "Group" else "Personal"}",
                        amount = CurrencyProvider.formatAmount(expense.amount),
                        icon = icon,
                        iconBgColor = iconColor.copy(alpha = 0.15f),
                        iconTint = iconColor,
                        onClick = { onExpenseClick(expense) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No expenses yet.\nTap + to add your first expense!", fontSize = 14.sp, color = FlexFiBodyText)
                    }
                }
            }
        }
    }
}

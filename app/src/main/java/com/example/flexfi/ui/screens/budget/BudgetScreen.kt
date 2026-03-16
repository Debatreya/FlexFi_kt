package com.example.flexfi.ui.screens.budget

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
import java.text.DateFormatSymbols

private val BUDGET_CATEGORIES = listOf(
    "OVERALL", "Food", "Transport", "Shopping", "Entertainment", "Utilities", "Health", "Rent", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudgetCategory by remember { mutableStateOf<String?>(null) }

    val monthName = DateFormatSymbols().months[state.month - 1]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "Budgets",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FlexFiFab(onClick = { showAddDialog = true })
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
            // Month navigator
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                            Icon(Icons.Default.ChevronLeft, "Previous month", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "$monthName ${state.year}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { viewModel.navigateMonth(1) }) {
                            Icon(Icons.Default.ChevronRight, "Next month", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Overall budget card
            state.overallBudget?.let { overall ->
                item {
                    FlexFiGradientCard {
                        Column {
                            Text("OVERALL BUDGET", fontSize = 11.sp, color = FlexFiWhite.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Spent", fontSize = 12.sp, color = FlexFiWhite.copy(alpha = 0.7f))
                                    Text(
                                        CurrencyProvider.formatAmount(overall.spentBase),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FlexFiWhite
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Limit", fontSize = 12.sp, color = FlexFiWhite.copy(alpha = 0.7f))
                                    Text(
                                        CurrencyProvider.formatAmount(overall.effectiveLimit),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FlexFiWhite
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FlexFiWhite.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(overall.percentage.coerceAtMost(1f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (overall.isOverBudget) FlexFiRed
                                            else if (overall.isWarning) FlexFiOrange
                                            else FlexFiWhite
                                        )
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${(overall.percentage * 100).toInt()}% used",
                                fontSize = 12.sp,
                                color = FlexFiWhite.copy(alpha = 0.8f)
                            )
                            if (overall.isOverBudget) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "⚠️ Over budget by ${CurrencyProvider.formatAmount(overall.spentBase - overall.effectiveLimit)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FlexFiRed
                                )
                            }
                        }
                    }
                }
            }

            // Overspend alerts
            val overBudgetItems = state.budgets.filter { it.isOverBudget }
            val warningItems = state.budgets.filter { it.isWarning }

            if (overBudgetItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FlexFiRedLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚨", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Over Budget!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = FlexFiRed
                                )
                                Text(
                                    overBudgetItems.joinToString(", ") { it.budget.category },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (warningItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FlexFiOrangeLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Approaching Limit (80%+)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = FlexFiOrange
                                )
                                Text(
                                    warningItems.joinToString(", ") { "${it.budget.category} (${(it.percentage * 100).toInt()}%)" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Category budgets header
            item {
                Text("Category Budgets", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            if (state.budgets.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No budgets set for $monthName.\nTap + to create your first budget!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Category budget cards
            items(state.budgets) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    item.budget.category,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${CurrencyProvider.formatAmount(item.spentBase)} / ${CurrencyProvider.formatAmount(item.effectiveLimit)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.budget.rollover) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FlexFiBlue.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            "Rollover",
                                            fontSize = 10.sp,
                                            color = FlexFiBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    "${(item.percentage * 100).toInt()}%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        item.isOverBudget -> FlexFiRed
                                        item.isWarning -> FlexFiOrange
                                        else -> FlexFiGreen
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(item.percentage.coerceAtMost(1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            item.isOverBudget -> FlexFiRed
                                            item.isWarning -> FlexFiOrange
                                            else -> FlexFiGreen
                                        }
                                    )
                            )
                        }
                        if (item.budget.rolledOverAmount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Includes ${CurrencyProvider.formatAmount(item.budget.rolledOverAmount)} rolled over",
                                fontSize = 11.sp,
                                color = FlexFiBlue
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Budget Dialog
    if (showAddDialog || editingBudgetCategory != null) {
        var dialogCategory by remember { mutableStateOf(editingBudgetCategory ?: BUDGET_CATEGORIES.first()) }
        var categoryExpanded by remember { mutableStateOf(false) }
        var limitInput by remember { mutableStateOf("") }
        var rollover by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingBudgetCategory = null
            },
            title = { Text(if (editingBudgetCategory != null) "Edit Budget" else "Set Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        FlexFiTextField(
                            value = dialogCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = "Category",
                            leadingIcon = Icons.Default.Category,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            BUDGET_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        dialogCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    FlexFiTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = "Budget Limit (${CurrencyProvider.symbol})",
                        placeholder = "0.00",
                        leadingIcon = Icons.Default.AttachMoney,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Rollover unused budget",
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = rollover,
                            onCheckedChange = { rollover = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val limit = limitInput.toDoubleOrNull()
                        if (limit != null && limit > 0) {
                            viewModel.addOrUpdateBudget(dialogCategory, limit, rollover)
                            showAddDialog = false
                            editingBudgetCategory = null
                        }
                    }
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingBudgetCategory = null
                }) { Text("Cancel") }
            }
        )
    }
}

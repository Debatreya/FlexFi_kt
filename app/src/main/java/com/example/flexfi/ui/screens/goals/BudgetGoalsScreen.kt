package com.example.flexfi.ui.screens.goals

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGoalsScreen(
    viewModel: BudgetGoalViewModel,
    onAddGoalClick: () -> Unit,
    onBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val totalSaved by viewModel.totalSaved.collectAsState()
    val totalTarget by viewModel.totalTarget.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    var showSavingsDialog by remember { mutableStateOf(false) }
    var selectedGoalId by remember { mutableStateOf("") }
    var savingsAmount by remember { mutableStateOf("") }

    // Add savings dialog
    if (showSavingsDialog) {
        AlertDialog(
            onDismissRequest = { showSavingsDialog = false },
            title = { Text("Add Savings") },
            text = {
                Column {
                    Text("How much would you like to save?", fontSize = 14.sp, color = FlexFiBodyText)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = savingsAmount,
                        onValueChange = { savingsAmount = it },
                        label = { Text("Amount") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        prefix = { Text(CurrencyProvider.symbol) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = savingsAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.addSavings(selectedGoalId, amount)
                        showSavingsDialog = false
                        savingsAmount = ""
                    }
                }) { Text("Add", color = FlexFiBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSavingsDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Your Goals",
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
            FlexFiFab(onClick = onAddGoalClick)
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

            // Total savings header
            item {
                FlexFiGradientCard {
                    Text("TOTAL SAVINGS PROGRESS", fontSize = 11.sp, color = FlexFiWhite.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CurrencyProvider.formatAmount(totalSaved),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlexFiWhite
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = FlexFiWhite.copy(alpha = 0.2f)) {
                            Text(
                                "${"%.0f".format(overallProgress * 100)}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlexFiWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "of ${CurrencyProvider.formatAmount(totalTarget)} target",
                        fontSize = 13.sp,
                        color = FlexFiWhite.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(FlexFiWhite.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(overallProgress)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(FlexFiWhite)
                        )
                    }
                }
            }

            // Active Goals
            item {
                Text("Active Goals", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
            }

            if (goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Flag, null, tint = FlexFiLightText, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No goals yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlexFiDarkText)
                            Text("Tap + to create your first savings goal!", fontSize = 13.sp, color = FlexFiBodyText)
                        }
                    }
                }
            } else {
                items(goals) { goal ->
                    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = FlexFiGreenLight,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Flag, null, tint = FlexFiGreen, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goal.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = FlexFiDarkText)
                                    if (goal.targetDate != null) {
                                        Text(
                                            "Target: ${dateFormat.format(Date(goal.targetDate))}",
                                            fontSize = 12.sp,
                                            color = FlexFiBodyText
                                        )
                                    }
                                }
                                Text(
                                    "${"%.0f".format(progress * 100)}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = FlexFiGreen
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FlexFiGreySurface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(FlexFiGreen)
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${CurrencyProvider.formatAmount(goal.savedAmount)} saved",
                                    fontSize = 12.sp,
                                    color = FlexFiBodyText
                                )
                                Text(
                                    "of ${CurrencyProvider.formatAmount(goal.targetAmount)}",
                                    fontSize = 12.sp,
                                    color = FlexFiBodyText
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        selectedGoalId = goal.id
                                        showSavingsDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = FlexFiGreenLight,
                                        contentColor = FlexFiGreen
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Savings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.deleteGoal(goal.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp), tint = FlexFiRed)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp, color = FlexFiRed)
                                }
                            }
                        }
                    }
                }
            }

            // Achievements
            if (achievements.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Achievements & Milestones", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(achievements) { achievement ->
                            Card(
                                modifier = Modifier.width(120.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (achievement.earned) FlexFiYellowLight else FlexFiGreySurface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(achievement.icon, fontSize = 28.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        achievement.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (achievement.earned) FlexFiDarkText else FlexFiLightText,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (achievement.earned) "Earned!" else "Locked",
                                        fontSize = 10.sp,
                                        color = if (achievement.earned) FlexFiGreen else FlexFiLightText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Auto-save section
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FlexFiWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = FlexFiBlue, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Save Schedule", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FlexFiDarkText)
                            Text("Set auto-save for your goals when creating them", fontSize = 12.sp, color = FlexFiBodyText)
                        }
                    }
                }
            }
        }
    }
}

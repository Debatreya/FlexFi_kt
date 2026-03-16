package com.example.flexfi.ui.screens.goals

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
import com.example.flexfi.ui.components.FlexFiTopBar
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    viewModel: BudgetGoalViewModel,
    onBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val goal = goals.find { it.id == goalId }
    val contributions by viewModel.selectedGoalContributions.collectAsState()

    LaunchedEffect(goalId) {
        viewModel.loadContributionsForGoal(goalId)
    }

    if (goal == null) {
        // Handle gracefully
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = goal.title,
                showBackButton = true,
                onBackClick = onBack
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

            // Goal Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FlexFiLightBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FlexFiWhite.copy(alpha = 0.9f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (goal.isSinkingFund) Icons.Default.Event else Icons.Default.Flag,
                                        null,
                                        tint = FlexFiBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Saved so far",
                                    fontSize = 12.sp,
                                    color = FlexFiDarkText.copy(alpha = 0.8f)
                                )
                                Text(
                                    CurrencyProvider.formatAmount(goal.savedAmount),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FlexFiBlue
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("${"%.0f".format(progress * 100)}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FlexFiBlue)
                            Text("Target: ${CurrencyProvider.formatAmount(goal.targetAmount)}", fontSize = 12.sp, color = FlexFiDarkText)
                        }

                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(FlexFiBlue.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FlexFiBlue)
                            )
                        }

                        // Sinking Fund Details
                        if (goal.isSinkingFund && goal.targetDate != null) {
                            Spacer(Modifier.height(20.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = FlexFiWhite.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Event, null, tint = FlexFiOrange, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Sinking Fund Target Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FlexFiDarkText)
                                        Text(dateFormat.format(Date(goal.targetDate)), fontSize = 13.sp, color = FlexFiOrange, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Description
            if (goal.description.isNotBlank()) {
                item {
                    Text("Description", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        goal.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Contributions History Section
            item {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Contribution History", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (contributions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null, tint = FlexFiLightText, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No contributions yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FlexFiDarkText)
                            Text("Add savings to see your history here.", fontSize = 12.sp, color = FlexFiBodyText)
                        }
                    }
                }
            } else {
                items(contributions) { contribution ->
                    val contribFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = FlexFiGreenLight,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.TrendingUp, null, tint = FlexFiGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (contribution.note.isNotBlank()) contribution.note else "Added Savings",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(contribFormat.format(Date(contribution.date)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "+${CurrencyProvider.formatAmount(contribution.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = FlexFiGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

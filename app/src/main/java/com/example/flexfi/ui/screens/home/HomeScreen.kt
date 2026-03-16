package com.example.flexfi.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGroupClick: (String) -> Unit,
    onContactsClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onPersonalExpensesClick: () -> Unit,
    onSettleUpClick: () -> Unit,
    onBudgetGoalsClick: () -> Unit,
    onAddExpenseClick: () -> Unit
) {
    val balances by viewModel.balances.collectAsState()
    val activeGroups by viewModel.activeGroups.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()
    val streak by viewModel.streak.collectAsState()
    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "FlexFi"
            )
        },
        bottomBar = {
            FlexFiBottomNavBar(
                currentTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        BottomNavTab.GROUPS -> onGroupsClick()
                        BottomNavTab.CONTACTS -> onContactsClick()
                        BottomNavTab.EXPENSES -> onPersonalExpensesClick()
                        BottomNavTab.PROFILE -> onProfileClick()
                        else -> {}
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Balance card
            item {
                FlexFiGradientCard {
                    Text("TOTAL BALANCE", fontSize = 11.sp, color = FlexFiWhite.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = CurrencyProvider.formatAmount(balances.totalBalance),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiWhite
                    )
                }
            }

            // Stat cards row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlexFiBalanceStatCard(
                        title = "YOU OWE",
                        amount = CurrencyProvider.formatAmount(balances.youOwe),
                        amountColor = FlexFiRed,
                        lineColor = FlexFiRed,
                        modifier = Modifier.weight(1f)
                    )
                    FlexFiBalanceStatCard(
                        title = "OWED TO YOU",
                        amount = CurrencyProvider.formatAmount(balances.owedToYou),
                        amountColor = FlexFiGreen,
                        lineColor = FlexFiGreen,
                        modifier = Modifier.weight(1f)
                    )
                    FlexFiBalanceStatCard(
                        title = "NET",
                        amount = CurrencyProvider.formatSigned(balances.net),
                        amountColor = if (balances.net >= 0) FlexFiGreen else FlexFiRed,
                        lineColor = FlexFiBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSettleUpClick,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Handshake, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Settle Up", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onBudgetGoalsClick,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Flag, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Goals", fontSize = 13.sp)
                    }
                }
            }

            // Active Groups
            if (activeGroups.isNotEmpty()) {
                item {
                    Text("Active Groups", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeGroups) { group ->
                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { onGroupClick(group.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    FlexFiAvatar(
                                        name = group.name,
                                        size = AvatarSize.MEDIUM
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = group.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Streak banner
            streak?.let { s ->
                if (s.currentStreak > 0) {
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
                                Text("🔥", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "${s.currentStreak}-Day Streak!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Keep logging expenses daily",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Activity
            if (recentActivity.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Activity", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = onPersonalExpensesClick) {
                            Text("See All", fontSize = 13.sp, color = FlexFiBlue)
                        }
                    }
                }
                items(recentActivity.take(5)) { expense ->
                    val icon = when (expense.category.lowercase()) {
                        "food" -> Icons.Default.Restaurant
                        "transport" -> Icons.Default.DirectionsCar
                        "shopping" -> Icons.Default.ShoppingBag
                        "entertainment" -> Icons.Default.SportsEsports
                        "health" -> Icons.Default.LocalHospital
                        "utilities" -> Icons.Default.Bolt
                        "rent" -> Icons.Default.Home
                        else -> Icons.Default.Receipt
                    }
                    val iconColor = when (expense.category.lowercase()) {
                        "food" -> CategoryFood
                        "transport" -> CategoryTransport
                        "shopping" -> CategoryShopping
                        "entertainment" -> CategoryEntertainment
                        "health" -> CategoryHealth
                        "utilities" -> CategoryUtilities
                        "rent" -> CategoryRent
                        else -> CategoryOther
                    }
                    FlexFiExpenseCard(
                        title = expense.description ?: "Expense",
                        subtitle = expense.category + " • " + (if (expense.source == "GROUP") "Group" else "Personal"),
                        amount = CurrencyProvider.formatTransactionAmount(
                            amount = expense.amount,
                            currency = expense.currency,
                            baseAmount = expense.baseAmount
                        ),
                        icon = icon,
                        iconBgColor = iconColor.copy(alpha = 0.15f),
                        iconTint = iconColor
                    )
                }
            }
        }
    }
}

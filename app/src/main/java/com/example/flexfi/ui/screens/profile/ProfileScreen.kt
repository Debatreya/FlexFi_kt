package com.example.flexfi.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.data.local.entities.RecurringTransactionEntity
import com.example.flexfi.ui.components.FlexFiAvatar
import com.example.flexfi.ui.components.FlexFiOutlinedButton
import com.example.flexfi.ui.components.FlexFiPrimaryButton
import com.example.flexfi.ui.components.FlexFiTextField
import com.example.flexfi.ui.components.FlexFiTopBar
import com.example.flexfi.ui.theme.FlexFiBodyText
import com.example.flexfi.ui.theme.FlexFiGreySurface
import com.example.flexfi.ui.theme.FlexFiWhite
import com.example.flexfi.utils.CurrencyProvider
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.flexfi.ui.theme.FlexFiGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val recurring by viewModel.recurring.collectAsState()
    val currentIncomeDisplay = CurrencyProvider.convertFromBase(settings.monthlyIncome)
    val currentBalanceDisplay = CurrencyProvider.convertFromBase(settings.currentBankBalance)

    var currencyExpanded by remember { mutableStateOf(false) }
    var monthlyIncomeInput by remember(settings.monthlyIncome, settings.displayCurrency) {
        mutableStateOf(String.format("%.2f", CurrencyProvider.convertFromBase(settings.monthlyIncome)))
    }
    var bankBalanceInput by remember(settings.currentBankBalance, settings.displayCurrency) {
        mutableStateOf(String.format("%.2f", CurrencyProvider.convertFromBase(settings.currentBankBalance)))
    }

    var showAddRecurring by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.updateProfilePhoto(uri?.toString())
    }

    if (showAddRecurring) {
        AddRecurringDialog(
            onDismiss = { showAddRecurring = false },
            onSave = { title, amount, currency, category, type, interval ->
                viewModel.addRecurring(title, amount, currency, category, type, interval)
                showAddRecurring = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "Profile & Settings",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlexFiAvatar(name = "You", imageUrl = settings.profilePhotoUri)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { photoPicker.launch("image/*") }) {
                            Text("Change Photo")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme", fontWeight = FontWeight.SemiBold)
                            Text("Toggle app appearance", fontSize = 12.sp, color = FlexFiBodyText)
                        }
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { viewModel.updateTheme(it) }
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded }
                    ) {
                        FlexFiTextField(
                            value = CurrencyProvider.getDisplayLabel(settings.displayCurrency),
                            onValueChange = {},
                            readOnly = true,
                            label = "Display Currency",
                            leadingIcon = Icons.Default.CurrencyExchange,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            CurrencyProvider.supportedCurrencies.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayLabel) },
                                    onClick = {
                                        viewModel.updateDisplayCurrency(option.code)
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    FlexFiTextField(
                        value = monthlyIncomeInput,
                        onValueChange = { monthlyIncomeInput = it },
                        label = "Monthly Income (${settings.displayCurrency})",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingContent = {
                            Text(
                                text = CurrencyProvider.getSymbol(settings.displayCurrency),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    FlexFiTextField(
                        value = bankBalanceInput,
                        onValueChange = { bankBalanceInput = it },
                        label = "Current Bank Balance (${settings.displayCurrency})",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingContent = {
                            Text(
                                text = CurrencyProvider.getSymbol(settings.displayCurrency),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    FlexFiPrimaryButton(
                        text = "Save Financial Settings",
                        onClick = {
                            val income = monthlyIncomeInput.toDoubleOrNull() ?: currentIncomeDisplay
                            val balance = bankBalanceInput.toDoubleOrNull() ?: currentBalanceDisplay
                            viewModel.saveFinancialSettings(income, balance)
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recurring Transactions", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showAddRecurring = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add recurring")
                        }
                    }

                    recurring.forEach { tx ->
                        RecurringRow(
                            tx = tx,
                            onToggle = { active -> viewModel.toggleRecurring(tx.id, active) },
                            onDelete = { viewModel.deleteRecurring(tx) }
                        )
                    }

                    if (recurring.isEmpty()) {
                        Text("No recurring items yet", color = FlexFiBodyText)
                    }

                    FlexFiOutlinedButton(
                        text = "Run Auto-Pay Now",
                        onClick = { viewModel.runRecurringNow() }
                    )
                }
            }

            ProfileActionCard(
                title = "Open Your Expenses",
                subtitle = "See full transactions and categories",
                icon = Icons.Default.ReceiptLong,
                onClick = onOpenExpenses
            )

            ProfileActionCard(
                title = "Open Analytics & Trends",
                subtitle = "Track insights and monthly patterns",
                icon = Icons.Default.BarChart,
                onClick = onOpenAnalytics
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(brush = FlexFiGradients.button),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RecurringRow(
    tx: RecurringTransactionEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${tx.title} (${tx.type})", fontWeight = FontWeight.SemiBold)
            Text("${tx.amount} ${tx.currency} • ${tx.interval}", fontSize = 12.sp, color = FlexFiBodyText)
        }
        Switch(checked = tx.isActive, onCheckedChange = onToggle)
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete recurring")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecurringDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, currency: String, category: String, type: String, interval: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var type by remember { mutableStateOf("EXPENSE") }
    var interval by remember { mutableStateOf("MONTHLY") }
    var currency by remember { mutableStateOf(CurrencyProvider.displayCurrencyCode) }

    var typeExpanded by remember { mutableStateOf(false) }
    var intervalExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlexFiTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    leadingIcon = Icons.Default.Person
                )
                FlexFiTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    leadingIcon = Icons.Default.AttachMoney,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                FlexFiTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = "Category"
                )

                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    FlexFiTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = "Type",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("INCOME", "EXPENSE").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { type = it; typeExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = intervalExpanded, onExpandedChange = { intervalExpanded = !intervalExpanded }) {
                    FlexFiTextField(
                        value = interval,
                        onValueChange = {},
                        readOnly = true,
                        label = "Interval",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = intervalExpanded, onDismissRequest = { intervalExpanded = false }) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { interval = it; intervalExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = !currencyExpanded }) {
                    FlexFiTextField(
                        value = CurrencyProvider.getDisplayLabel(currency),
                        onValueChange = {},
                        readOnly = true,
                        label = "Currency",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                        CurrencyProvider.supportedCurrencies.forEach { option ->
                            DropdownMenuItem(text = { Text(option.displayLabel) }, onClick = { currency = option.code; currencyExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = amount.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && parsed > 0.0) {
                    onSave(title.trim(), parsed, currency, category.ifBlank { "Other" }, type, interval)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.example.flexfi.ui.screens.settle

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    viewModel: SettleUpViewModel,
    groupId: String? = null,
    onBack: () -> Unit
) {
    LaunchedEffect(groupId) {
        viewModel.loadAllDebts(groupId)
    }

    val debts by viewModel.debts.collectAsState()
    val totalYouOwe by viewModel.totalYouOwe.collectAsState()
    val totalOwedToYou by viewModel.totalOwedToYou.collectAsState()
    val pendingPayments by viewModel.pendingPayments.collectAsState()
    val context = LocalContext.current

    var showPayDialog by remember { mutableStateOf(false) }
    var selectedDebt by remember { mutableStateOf<SettleUpDebt?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var payCurrency by remember { mutableStateOf(CurrencyProvider.displayCurrencyCode) }
    var currencyExpanded by remember { mutableStateOf(false) }

    // Pay dialog
    if (showPayDialog && selectedDebt != null) {
        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text("Record Payment") },
            text = {
                Column {
                    Text(
                        "How much did you pay ${selectedDebt!!.personName}?",
                        fontSize = 14.sp,
                        color = FlexFiBodyText
                    )
                    Text(
                        "Total owed: ${CurrencyProvider.formatAmount(selectedDebt!!.amountBase)}",
                        fontSize = 12.sp,
                        color = FlexFiLightText
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Amount Paid") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        prefix = { Text(payCurrency) }
                    )
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded }
                    ) {
                        OutlinedTextField(
                            value = payCurrency,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Payment Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            CurrencyProvider.supportedCurrencyCodes.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code) },
                                    onClick = {
                                        payCurrency = code
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { payAmount = "%.2f".format(CurrencyProvider.convertFromBase(selectedDebt!!.amountBase)) },
                            label = { Text("Full Amount") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        AssistChip(
                            onClick = { payAmount = "%.2f".format(CurrencyProvider.convertFromBase(selectedDebt!!.amountBase / 2.0)) },
                            label = { Text("Half") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Note: Actual payment is done outside the app using your preferred payment method.",
                        fontSize = 11.sp,
                        color = FlexFiLightText
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = payAmount.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        Toast.makeText(context, "Enter valid amount", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    viewModel.recordPayment(
                        toPhone = selectedDebt!!.personPhone,
                        amount = amount,
                        currency = payCurrency,
                        groupId = selectedDebt!!.groupId,
                        onSuccess = {
                            showPayDialog = false
                            payAmount = ""
                            Toast.makeText(context, "Payment recorded!", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    )
                }) {
                    Text("Record", color = FlexFiBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Settle Up",
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
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Summary card
            item {
                FlexFiGradientCard {
                    Text("TOTAL TO SETTLE", fontSize = 11.sp, color = FlexFiWhite.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = CurrencyProvider.formatAmount(totalYouOwe),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiWhite
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = FlexFiWhite.copy(alpha = 0.2f)) {
                            Text(
                                "You owe: ${CurrencyProvider.formatAmount(totalYouOwe)}",
                                fontSize = 11.sp,
                                color = FlexFiWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = FlexFiWhite.copy(alpha = 0.2f)) {
                            Text(
                                "Owed to you: ${CurrencyProvider.formatAmount(totalOwedToYou)}",
                                fontSize = 11.sp,
                                color = FlexFiWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Pending payments section
            if (pendingPayments.isNotEmpty()) {
                item {
                    Text("Pending Approvals", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
                }
                items(pendingPayments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlexFiAvatar(name = payment.otherName, size = AvatarSize.MEDIUM)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(payment.otherName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FlexFiDarkText)
                                Text(
                                    if (payment.isIncoming) "Paid you" else "You paid them",
                                    fontSize = 12.sp,
                                    color = FlexFiBodyText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    CurrencyProvider.formatAmount(payment.amount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (payment.isIncoming) FlexFiGreen else FlexFiBodyText
                                )
                                Spacer(Modifier.height(4.dp))
                                if (payment.isIncoming) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = { viewModel.rejectPayment(payment.id) },
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) { Text("Reject", fontSize = 12.sp, color = FlexFiRed) }
                                        FilledTonalButton(
                                            onClick = { viewModel.acceptPayment(payment.id) },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = FlexFiGreenLight,
                                                contentColor = FlexFiGreen
                                            )
                                        ) { Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = FlexFiLightBlue.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            "Pending",
                                            fontSize = 11.sp,
                                            color = FlexFiBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Friends & Groups", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FlexFiDarkText)
                }
            }

            // Debts list
            if (debts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, tint = FlexFiGreen, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("All settled up!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlexFiDarkText)
                            Text("You don't owe anyone", fontSize = 13.sp, color = FlexFiBodyText)
                        }
                    }
                }
            } else {
                items(debts) { debt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlexFiAvatar(name = debt.personName, size = AvatarSize.MEDIUM)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(debt.personName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FlexFiDarkText)
                                Text(
                                    if (debt.youOwe) "You owe" else "Owes you",
                                    fontSize = 12.sp,
                                    color = FlexFiBodyText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    CurrencyProvider.formatAmount(debt.amountBase),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (debt.youOwe) FlexFiRed else FlexFiGreen
                                )
                                Spacer(Modifier.height(4.dp))
                                if (debt.youOwe) {
                                    FilledTonalButton(
                                        onClick = {
                                            selectedDebt = debt
                                            payAmount = "%.2f".format(CurrencyProvider.convertFromBase(debt.amountBase))
                                            showPayDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = FlexFiGreenLight,
                                            contentColor = FlexFiGreen
                                        )
                                    ) { Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                }
                            }
                        }
                    }
                }
            }

            // Settle All button
            if (debts.any { it.youOwe }) {
                item {
                    Spacer(Modifier.height(8.dp))
                    FlexFiPrimaryButton(
                        text = "Settle All Debts",
                        onClick = {
                            // Record all debts
                            debts.filter { it.youOwe }.forEach { debt ->
                                viewModel.recordPaymentInBase(
                                    toPhone = debt.personPhone,
                                    amountBase = debt.amountBase,
                                    groupId = debt.groupId,
                                    onSuccess = {},
                                    onError = {}
                                )
                            }
                            Toast.makeText(context, "All debts settled!", Toast.LENGTH_LONG).show()
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowForward, null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
    }
}

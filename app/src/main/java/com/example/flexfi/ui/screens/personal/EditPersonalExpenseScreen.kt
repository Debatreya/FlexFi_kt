package com.example.flexfi.ui.screens.personal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
import java.text.SimpleDateFormat
import java.util.*

private val EDIT_CATEGORIES = listOf(
    "Food", "Transport", "Shopping", "Entertainment", "Utilities", "Health", "Rent", "Other"
)

private val EDIT_SUB_CATEGORIES = mapOf(
    "Food" to listOf("Groceries", "Dining Out", "Coffee", "Delivery", "Snacks"),
    "Transport" to listOf("Fuel", "Public Transit", "Ride Share", "Parking", "Maintenance"),
    "Shopping" to listOf("Clothing", "Electronics", "Home", "Gifts", "Online"),
    "Entertainment" to listOf("Movies", "Games", "Music", "Sports", "Events"),
    "Utilities" to listOf("Electricity", "Water", "Internet", "Phone", "Gas"),
    "Health" to listOf("Medicine", "Doctor", "Gym", "Insurance", "Supplements"),
    "Rent" to listOf("Apartment", "Office", "Storage"),
    "Other" to listOf("Miscellaneous", "Donation", "Fees", "Education")
)

private val EDIT_TRANSACTION_TYPES = listOf("EXPENSE", "INCOME", "TRANSFER")
private val EDIT_PAYMENT_MODES = listOf("Cash", "Credit Card", "Debit Card", "UPI")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalExpenseScreen(
    expense: PersonalExpenseEntity,
    viewModel: PersonalExpenseViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(expense.description ?: "") }
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var selectedCurrency by remember { mutableStateOf(expense.currency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember {
        mutableStateOf(
            if (expense.category in EDIT_CATEGORIES) expense.category else "Other"
        )
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedSubCategory by remember { mutableStateOf(expense.subCategory ?: "") }
    var subCategoryExpanded by remember { mutableStateOf(false) }

    // New fields pre-populated from expense
    var selectedType by remember { mutableStateOf(expense.type) }
    var typeExpanded by remember { mutableStateOf(false) }
    var selectedPaymentMode by remember { mutableStateOf(expense.paymentMode) }
    var paymentModeExpanded by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf(expense.merchant ?: "") }
    var tagsInput by remember { mutableStateOf(expense.tags ?: "") }
    var attachmentUri by remember { mutableStateOf(expense.attachmentUri?.let { Uri.parse(it) }) }

    var selectedDateMillis by remember { mutableStateOf(expense.createdAt) }
    val dateLabel = remember(selectedDateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) attachmentUri = uri }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Remove \"${expense.description}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteExpense(
                            id = expense.id,
                            onSuccess = { onBack() },
                            onError = { msg -> errorMessage = msg }
                        )
                    }
                ) { Text("Delete", color = FlexFiRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "Edit Transaction",
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

            // Amount display card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (selectedType) {
                            "INCOME" -> "INCOME TOTAL"
                            "TRANSFER" -> "TRANSFER TOTAL"
                            else -> "EXPENSE TOTAL"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${CurrencyProvider.getSymbol(selectedCurrency)} ${amount.ifEmpty { "0" }}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (selectedType) {
                            "INCOME" -> FlexFiGreen
                            "TRANSFER" -> FlexFiBlue
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            // Transaction Type selector
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                FlexFiTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = "Transaction Type",
                    leadingIcon = Icons.Default.SwapHoriz,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    EDIT_TRANSACTION_TYPES.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = { selectedType = type; typeExpanded = false }
                        )
                    }
                }
            }

            // Currency dropdown
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = !currencyExpanded }
            ) {
                FlexFiTextField(
                    value = CurrencyProvider.getDisplayLabel(selectedCurrency),
                    onValueChange = {},
                    readOnly = true,
                    label = "Currency",
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
                                selectedCurrency = option.code
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)

                    FlexFiTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title / Description",
                        leadingIcon = Icons.Default.Edit
                    )

                    FlexFiTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount (${CurrencyProvider.getSymbol(selectedCurrency)})",
                        leadingIcon = Icons.Default.AttachMoney,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    // Category dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        FlexFiTextField(
                            value = selectedCategory,
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
                            EDIT_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        selectedSubCategory = ""
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Sub-category dropdown
                    val subCategories = EDIT_SUB_CATEGORIES[selectedCategory] ?: emptyList()
                    if (subCategories.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = subCategoryExpanded,
                            onExpandedChange = { subCategoryExpanded = !subCategoryExpanded }
                        ) {
                            FlexFiTextField(
                                value = selectedSubCategory.ifEmpty { "Select Sub-Category" },
                                onValueChange = {},
                                readOnly = true,
                                label = "Sub-Category",
                                leadingIcon = Icons.Default.Sell,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = subCategoryExpanded,
                                onDismissRequest = { subCategoryExpanded = false }
                            ) {
                                subCategories.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub) },
                                        onClick = {
                                            selectedSubCategory = sub
                                            subCategoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Payment Mode dropdown
                    ExposedDropdownMenuBox(
                        expanded = paymentModeExpanded,
                        onExpandedChange = { paymentModeExpanded = !paymentModeExpanded }
                    ) {
                        FlexFiTextField(
                            value = selectedPaymentMode,
                            onValueChange = {},
                            readOnly = true,
                            label = "Payment Mode",
                            leadingIcon = Icons.Default.Payment,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentModeExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = paymentModeExpanded,
                            onDismissRequest = { paymentModeExpanded = false }
                        ) {
                            EDIT_PAYMENT_MODES.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = {
                                        selectedPaymentMode = mode
                                        paymentModeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Merchant/Payee
                    FlexFiTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = "Merchant / Payee",
                        placeholder = "e.g. Amazon, Starbucks",
                        leadingIcon = Icons.Default.Store
                    )

                    // Tags
                    FlexFiTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = "Tags (comma separated)",
                        placeholder = "#Vacation2024, #WorkRelated",
                        leadingIcon = Icons.Default.Tag
                    )

                    // Date
                    FlexFiTextField(
                        value = dateLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = "Date",
                        leadingIcon = Icons.Default.CalendarToday,
                        trailingIcon = {
                            TextButton(onClick = { showDatePicker = true }) { Text("Change", color = FlexFiBlue) }
                        }
                    )

                    // Attachment
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlexFiOutlinedButton(
                            text = if (attachmentUri != null) "Receipt Attached ✓" else "Attach Receipt",
                            onClick = { photoPickerLauncher.launch("image/*") }
                        )
                        if (attachmentUri != null) {
                            TextButton(onClick = { attachmentUri = null }) {
                                Text("Remove", color = FlexFiRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Error
            errorMessage?.let {
                Text(text = it, color = FlexFiRed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(4.dp))

            FlexFiPrimaryButton(
                text = if (isSaving) "Saving…" else "Save Changes",
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    when {
                        title.isBlank() -> errorMessage = "Please enter a title"
                        parsedAmount == null || parsedAmount <= 0 -> errorMessage = "Please enter a valid amount"
                        else -> {
                            isSaving = true
                            errorMessage = null
                            viewModel.updateExpense(
                                expense = expense,
                                title = title.trim(),
                                amount = parsedAmount,
                                currency = selectedCurrency,
                                category = selectedCategory,
                                dateMillis = selectedDateMillis,
                                onSuccess = { onBack() },
                                onError = { msg ->
                                    errorMessage = msg
                                    isSaving = false
                                }
                            )
                        }
                    }
                },
                enabled = !isSaving,
                trailingIcon = {
                    Icon(Icons.Default.Check, null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
                }
            )

            // Only show delete for manually entered expenses
            if (expense.source == "PERSONAL") {
                FlexFiOutlinedButton(
                    text = "Delete Expense",
                    onClick = { showDeleteDialog = true }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

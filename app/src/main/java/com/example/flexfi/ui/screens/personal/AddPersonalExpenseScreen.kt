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
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
import com.example.flexfi.utils.ExpenseCategorizer
import java.text.SimpleDateFormat
import java.util.*

private val CATEGORIES = listOf(
    "Food", "Transport", "Shopping", "Entertainment", "Utilities", "Health", "Rent", "Other"
)

private val SUB_CATEGORIES = mapOf(
    "Food" to listOf("Groceries", "Dining Out", "Coffee", "Delivery", "Snacks"),
    "Transport" to listOf("Fuel", "Public Transit", "Ride Share", "Parking", "Maintenance"),
    "Shopping" to listOf("Clothing", "Electronics", "Home", "Gifts", "Online"),
    "Entertainment" to listOf("Movies", "Games", "Music", "Sports", "Events"),
    "Utilities" to listOf("Electricity", "Water", "Internet", "Phone", "Gas"),
    "Health" to listOf("Medicine", "Doctor", "Gym", "Insurance", "Supplements"),
    "Rent" to listOf("Apartment", "Office", "Storage"),
    "Other" to listOf("Miscellaneous", "Donation", "Fees", "Education")
)

private val TRANSACTION_TYPES = listOf("EXPENSE", "INCOME", "TRANSFER")
private val PAYMENT_MODES = listOf("Cash", "Credit Card", "Debit Card", "UPI")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalExpenseScreen(
    viewModel: PersonalExpenseViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(CurrencyProvider.displayCurrencyCode) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(CATEGORIES.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedSubCategory by remember { mutableStateOf("") }
    var subCategoryExpanded by remember { mutableStateOf(false) }

    // New fields
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var typeExpanded by remember { mutableStateOf(false) }
    var selectedPaymentMode by remember { mutableStateOf("Cash") }
    var paymentModeExpanded by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }

    // Date defaults to today; stored as epoch millis
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateLabel = remember(selectedDateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Photo picker for receipt attachment
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> attachmentUri = uri }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "Add Transaction",
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
                    TRANSACTION_TYPES.forEach { type ->
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
                        onValueChange = {
                            title = it
                            ExpenseCategorizer.categorize(it)?.let { detectedCategory ->
                                selectedCategory = detectedCategory
                            }
                        },
                        label = "Title / Description",
                        placeholder = "e.g. Vegetables, Uber Ride",
                        leadingIcon = Icons.Default.Edit
                    )

                    FlexFiTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount (${CurrencyProvider.getSymbol(selectedCurrency)})",
                        placeholder = "0.00",
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
                            CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        selectedSubCategory = "" // reset sub-category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Sub-category dropdown (changes based on selected category)
                    val subCategories = SUB_CATEGORIES[selectedCategory] ?: emptyList()
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
                            PAYMENT_MODES.forEach { mode ->
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

                    // Merchant/Payee field
                    FlexFiTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = "Merchant / Payee",
                        placeholder = "e.g. Amazon, Starbucks",
                        leadingIcon = Icons.Default.Store
                    )

                    // Tags field
                    FlexFiTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = "Tags (comma separated)",
                        placeholder = "#Vacation2024, #WorkRelated",
                        leadingIcon = Icons.Default.Tag
                    )

                    // Date picker
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

                    // Attachment button
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
                Text(
                    text = it,
                    color = FlexFiRed,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            FlexFiPrimaryButton(
                text = if (isSaving) "Saving…" else "Save ${selectedType.lowercase().replaceFirstChar { it.uppercase() }}",
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    when {
                        title.isBlank() -> errorMessage = "Please enter a title"
                        parsedAmount == null || parsedAmount <= 0 -> errorMessage = "Please enter a valid amount"
                        else -> {
                            isSaving = true
                            errorMessage = null
                            viewModel.addManualExpense(
                                title = title.trim(),
                                amount = parsedAmount,
                                currency = selectedCurrency,
                                category = selectedCategory,
                                dateMillis = selectedDateMillis,
                                subCategory = selectedSubCategory.ifBlank { null },
                                type = selectedType,
                                paymentMode = selectedPaymentMode,
                                merchant = merchant.ifBlank { null },
                                tags = tagsInput.ifBlank { null },
                                attachmentUri = attachmentUri?.toString(),
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

            Spacer(Modifier.height(24.dp))
        }
    }
}

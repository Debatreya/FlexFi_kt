package com.example.flexfi.ui.screens.expenses

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flexfi.data.local.dao.GroupMemberInfo
import com.example.flexfi.ui.screens.groups.GroupViewModel
import com.example.flexfi.utils.ExpenseCategorizer

private val CATEGORIES = listOf(
    "Food", "Transport", "Shopping", "Entertainment", "Utilities", "Health", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    groupId: String,
    expenseViewModel: ExpenseViewModel,
    groupViewModel: GroupViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUserPhone = groupViewModel.currentUserPhone
    val members by groupViewModel.getGroupMembers(groupId).collectAsState(initial = emptyList())

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var paidByPhone by remember { mutableStateOf(currentUserPhone) }
    
    // Split Type: "equal" or "exact"
    var splitType by remember { mutableStateOf("equal") }
    
    // Member selection and exact amounts
    var selectedPhones by remember { mutableStateOf<Set<String>>(emptySet()) }
    var exactAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    var showPayerDropdown by remember { mutableStateOf(false) }

    // Initialize selected members once loaded
    LaunchedEffect(members) {
        if (members.isNotEmpty() && selectedPhones.isEmpty()) {
            selectedPhones = members.map { it.phone }.toSet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { 
                    title = it
                    ExpenseCategorizer.categorize(it)?.let { detectedCategory ->
                        category = detectedCategory
                    }
                },
                label = { Text("Title (e.g. Dinner)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Split Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = splitType == "equal",
                    onClick = { splitType = "equal" },
                    label = { Text("Equal Split") }
                )
                FilterChip(
                    selected = splitType == "exact",
                    onClick = { splitType = "exact" },
                    label = { Text("Exact Amounts") }
                )
            }

            // Paid By Dropdown
            ExposedDropdownMenuBox(
                expanded = showPayerDropdown,
                onExpandedChange = { showPayerDropdown = it }
            ) {
                val selectedMember = members.find { it.phone == paidByPhone }
                val displayName = when {
                    paidByPhone == currentUserPhone -> "Me"
                    selectedMember?.contactName != null -> selectedMember.contactName
                    else -> paidByPhone
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paid By") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPayerDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = showPayerDropdown,
                    onDismissRequest = { showPayerDropdown = false }
                ) {
                    members.forEach { member ->
                        val text = if (member.phone == currentUserPhone) "Me" 
                                   else member.contactName ?: member.phone
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                paidByPhone = member.phone
                                showPayerDropdown = false
                            }
                        )
                    }
                }
            }

            Text("Split Between:", style = MaterialTheme.typography.titleMedium)

            // Members List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members) { member ->
                    val isSelected = selectedPhones.contains(member.phone)
                    val displayName = if (member.phone == currentUserPhone) "Me" else member.contactName ?: member.phone

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedPhones = selectedPhones + member.phone
                                } else {
                                    selectedPhones = selectedPhones - member.phone
                                }
                            }
                        )
                        Text(text = displayName, modifier = Modifier.weight(1f))
                        
                        if (splitType == "exact") {
                            OutlinedTextField(
                                value = exactAmounts[member.phone] ?: "",
                                onValueChange = { newValue ->
                                    exactAmounts = exactAmounts.toMutableMap().apply {
                                        put(member.phone, newValue)
                                    }
                                    if (newValue.isNotEmpty()) {
                                        selectedPhones = selectedPhones + member.phone
                                    }
                                },
                                label = { Text("₹") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isBlank() || amount == null || amount <= 0) {
                        Toast.makeText(context, "Please enter valid title and amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedPhones.isEmpty()) {
                        Toast.makeText(context, "Select at least one member", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val exactAmountMap = if (splitType == "exact") {
                        val map = mutableMapOf<String, Double>()
                        for (phone in selectedPhones) {
                            val v = exactAmounts[phone]?.toDoubleOrNull() ?: 0.0
                            map[phone] = v
                        }
                        map
                    } else null

                    expenseViewModel.addExpense(
                        title = title,
                        amount = amount,
                        groupId = groupId,
                        paidByPhone = paidByPhone,
                        category = category,
                        splitType = splitType,
                        selectedMemberPhones = selectedPhones.toList(),
                        exactAmounts = exactAmountMap,
                        onSuccess = { onBack() },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Expense")
            }
        }
    }
}

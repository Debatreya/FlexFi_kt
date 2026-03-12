package com.example.flexfi.ui.screens.expenses

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.data.local.dao.GroupMemberInfo
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.screens.groups.GroupViewModel
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider
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
    var amountText by remember { mutableStateOf("0") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var paidByPhone by remember { mutableStateOf(currentUserPhone) }
    var splitType by remember { mutableStateOf("equal") }
    var selectedPhones by remember { mutableStateOf<Set<String>>(emptySet()) }
    var exactAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(members) {
        if (members.isNotEmpty() && selectedPhones.isEmpty()) {
            selectedPhones = members.map { it.phone }.toSet()
        }
    }

    val memberNames = remember(members, currentUserPhone) {
        members.associate {
            it.phone to if (it.phone == currentUserPhone) "Me" else it.contactName ?: it.phone
        }
    }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Add Expense",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Receipt, "Receipt", tint = FlexFiDarkText)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Amount display card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("EXPENSE TOTAL", fontSize = 11.sp, color = FlexFiBodyText, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${CurrencyProvider.symbol}$amountText",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiDarkText
                    )
                }
            }

            // Title
            FlexFiTextField(
                value = title,
                onValueChange = {
                    title = it
                    ExpenseCategorizer.categorize(it)?.let { det -> category = det }
                },
                label = "What was the expense for?",
                leadingIcon = Icons.Default.Edit
            )

            // Category
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                FlexFiTextField(
                    value = category,
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
                            onClick = { category = cat; categoryExpanded = false }
                        )
                    }
                }
            }

            // Paid by section
            Text("PAID BY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlexFiBodyText, letterSpacing = 1.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(members) { member ->
                    val name = memberNames[member.phone] ?: member.phone
                    val isSelected = member.phone == paidByPhone
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) FlexFiBlue else FlexFiGreySurface,
                        modifier = Modifier.clickable { paidByPhone = member.phone }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlexFiAvatar(name = name, size = AvatarSize.SMALL)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                name.take(10),
                                color = if (isSelected) FlexFiWhite else FlexFiDarkText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Split type tabs
            Text("SPLIT TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlexFiBodyText, letterSpacing = 1.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("equal" to "Equally", "exact" to "Exact").forEach { (type, label) ->
                    FilterChip(
                        selected = splitType == type,
                        onClick = { splitType = type },
                        label = { Text(label, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlexFiBlue,
                            selectedLabelColor = FlexFiWhite
                        )
                    )
                }
            }

            // Member selection
            Text("SPLIT BETWEEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlexFiBodyText, letterSpacing = 1.sp)
            members.forEach { member ->
                val name = memberNames[member.phone] ?: member.phone
                val isSelected = selectedPhones.contains(member.phone)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FlexFiWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedPhones = if (checked) selectedPhones + member.phone
                                                else selectedPhones - member.phone
                            },
                            colors = CheckboxDefaults.colors(checkedColor = FlexFiBlue)
                        )
                        FlexFiAvatar(name = name, size = AvatarSize.SMALL)
                        Spacer(Modifier.width(10.dp))
                        Text(name, modifier = Modifier.weight(1f), fontSize = 14.sp, color = FlexFiDarkText)
                        if (splitType == "exact") {
                            OutlinedTextField(
                                value = exactAmounts[member.phone] ?: "",
                                onValueChange = { v ->
                                    exactAmounts = exactAmounts.toMutableMap().apply { put(member.phone, v) }
                                    if (v.isNotEmpty()) selectedPhones = selectedPhones + member.phone
                                },
                                label = { Text(CurrencyProvider.symbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(90.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Amount keypad
            Spacer(Modifier.height(4.dp))
            FlexFiNumericKeypad(
                onKeyPress = { key ->
                    amountText = if (amountText == "0") key else amountText + key
                },
                onDelete = {
                    amountText = if (amountText.length <= 1) "0" else amountText.dropLast(1)
                }
            )

            Spacer(Modifier.height(8.dp))

            FlexFiPrimaryButton(
                text = "Save Expense",
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isBlank() || amount == null || amount <= 0) {
                        Toast.makeText(context, "Enter valid title and amount", Toast.LENGTH_SHORT).show()
                        return@FlexFiPrimaryButton
                    }
                    if (selectedPhones.isEmpty()) {
                        Toast.makeText(context, "Select at least one member", Toast.LENGTH_SHORT).show()
                        return@FlexFiPrimaryButton
                    }
                    val exactMap = if (splitType == "exact") {
                        selectedPhones.associateWith { (exactAmounts[it]?.toDoubleOrNull() ?: 0.0) }
                    } else null

                    expenseViewModel.addExpense(
                        title = title, amount = amount, groupId = groupId,
                        paidByPhone = paidByPhone, category = category,
                        splitType = splitType, selectedMemberPhones = selectedPhones.toList(),
                        exactAmounts = exactMap,
                        onSuccess = { onBack() },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                },
                trailingIcon = {
                    Icon(Icons.Default.Check, null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

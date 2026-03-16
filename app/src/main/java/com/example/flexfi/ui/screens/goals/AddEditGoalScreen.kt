package com.example.flexfi.ui.screens.goals

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    viewModel: BudgetGoalViewModel,
    goalId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var autoSaveAmount by remember { mutableStateOf("") }
    var autoSaveFrequency by remember { mutableStateOf("monthly") }
    var isEditing by remember { mutableStateOf(false) }
    var isSinkingFund by remember { mutableStateOf(false) }
    var targetDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateLabel = remember(targetDate) {
        targetDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select target date"
    }

    // Load existing goal if editing
    LaunchedEffect(goalId) {
        if (goalId != null) {
            val goal = viewModel.getGoalById(goalId)
            if (goal != null) {
                isEditing = true
                title = goal.title
                description = goal.description
                targetAmount = "%.2f".format(goal.targetAmount)
                autoSaveAmount = if (goal.autoSaveAmount > 0) "%.2f".format(goal.autoSaveAmount) else ""
                autoSaveFrequency = goal.autoSaveFrequency
                isSinkingFund = goal.isSinkingFund
                targetDate = goal.targetDate
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = if (isEditing) "Edit Goal" else "Create Goal",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = targetDate ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { targetDate = it }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("GOAL DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)

                    FlexFiTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Goal Title",
                        placeholder = "e.g. Emergency Fund",
                        leadingIcon = Icons.Default.Flag
                    )

                    FlexFiTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description (Optional)",
                        placeholder = "What is this goal for?",
                        leadingIcon = Icons.Default.Description
                    )

                    FlexFiTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = "Target Amount",
                        placeholder = "0.00",
                        leadingIcon = Icons.Default.AttachMoney,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Auto-save settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("AUTO-SAVE (OPTIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)

                    FlexFiTextField(
                        value = autoSaveAmount,
                        onValueChange = { autoSaveAmount = it },
                        label = "Auto-Save Amount",
                        placeholder = "0.00",
                        leadingIcon = Icons.Default.Savings,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("Frequency", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("weekly" to "Weekly", "monthly" to "Monthly").forEach { (freq, label) ->
                            FilterChip(
                                selected = autoSaveFrequency == freq,
                                onClick = { autoSaveFrequency = freq },
                                label = { Text(label) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FlexFiBlue,
                                    selectedLabelColor = FlexFiWhite
                                )
                            )
                        }
                    }
                }
            }

            // Sinking Fund settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SINKING FUND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text("Set a target date to reach this goal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isSinkingFund,
                            onCheckedChange = { isSinkingFund = it }
                        )
                    }

                    if (isSinkingFund) {
                        FlexFiTextField(
                            value = dateLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = "Target Date",
                            leadingIcon = Icons.Default.CalendarToday,
                            trailingIcon = {
                                TextButton(onClick = { showDatePicker = true }) { Text("Change", color = FlexFiBlue) }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            FlexFiPrimaryButton(
                text = if (isEditing) "Update Goal" else "Create Goal",
                onClick = {
                    val target = targetAmount.toDoubleOrNull()
                    if (title.isBlank() || target == null || target <= 0) {
                        Toast.makeText(context, "Enter valid title and target amount", Toast.LENGTH_SHORT).show()
                        return@FlexFiPrimaryButton
                    }
                    val autoSave = autoSaveAmount.toDoubleOrNull() ?: 0.0

                    if (isEditing && goalId != null) {
                        scope.launch {
                            val existing = viewModel.getGoalById(goalId)
                            if (existing != null) {
                                viewModel.updateGoal(
                                    existing.copy(
                                        title = title,
                                        description = description,
                                        targetAmount = target,
                                        autoSaveAmount = autoSave,
                                        autoSaveFrequency = autoSaveFrequency,
                                        isSinkingFund = isSinkingFund,
                                        targetDate = if (isSinkingFund) targetDate else null
                                    )
                                )
                            }
                            onBack()
                        }
                    } else {
                        viewModel.createGoal(
                            title = title,
                            description = description,
                            targetAmount = target,
                            targetDate = if (isSinkingFund) targetDate else null,
                            autoSaveAmount = autoSave,
                            autoSaveFrequency = autoSaveFrequency,
                            isSinkingFund = isSinkingFund,
                            onSuccess = { onBack() },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                trailingIcon = {
                    Icon(Icons.Default.Check, null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

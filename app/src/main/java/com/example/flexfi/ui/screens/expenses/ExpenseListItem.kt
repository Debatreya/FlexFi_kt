package com.example.flexfi.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flexfi.data.local.entities.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseListItem(
    expense: ExpenseEntity,
    currentUserPhone: String,
    memberNames: Map<String, String> // phone -> Display Name
) {
    val date = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(expense.createdAt))
    val payerName = memberNames[expense.paidByPhone] ?: expense.paidByPhone

    ListItem(
        headlineContent = { Text(expense.title) },
        leadingContent = {
            Icon(Icons.Default.Receipt, contentDescription = "Expense")
        },
        supportingContent = {
            Column {
                Text("Paid by $payerName")
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        trailingContent = {
            Text(
                text = "₹${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    )
}

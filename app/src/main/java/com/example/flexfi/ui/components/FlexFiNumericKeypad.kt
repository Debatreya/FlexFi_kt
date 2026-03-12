package com.example.flexfi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.theme.FlexFiBodyText
import com.example.flexfi.ui.theme.FlexFiDarkText
import com.example.flexfi.ui.theme.FlexFiGreySurface

@Composable
fun FlexFiNumericKeypad(
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDecimal: Boolean = true
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showDecimal) "." else "", "0", "DEL")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (key.isNotEmpty()) FlexFiGreySurface else androidx.compose.ui.graphics.Color.Transparent)
                            .then(
                                if (key.isNotEmpty()) Modifier.clickable {
                                    if (key == "DEL") onDelete() else onKeyPress(key)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "DEL") {
                            Icon(
                                Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = FlexFiBodyText,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = FlexFiDarkText
                            )
                        }
                    }
                }
            }
        }
    }
}

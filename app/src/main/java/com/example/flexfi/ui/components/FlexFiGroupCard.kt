package com.example.flexfi.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.theme.*

@Composable
fun FlexFiGroupCard(
    name: String,
    memberCount: Int,
    totalBalance: String,
    balanceColor: Color,
    latestActivity: String,
    groupIcon: @Composable () -> Unit,
    memberAvatars: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: icon + name + balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                groupIcon()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiDarkText
                    )
                    Text(
                        text = "$memberCount members",
                        fontSize = 12.sp,
                        color = FlexFiBodyText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL BALANCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlexFiBodyText
                    )
                    Text(
                        text = totalBalance,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom row: avatars + activity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                memberAvatars()
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FlexFiGreySurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🕐 ", fontSize = 11.sp)
                        Text(
                            text = latestActivity,
                            fontSize = 11.sp,
                            color = FlexFiBodyText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

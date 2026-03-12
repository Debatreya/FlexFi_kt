package com.example.flexfi.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigate: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_progress")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        onNavigate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlexFiGradients.splash),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo icon
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FlexFiWhite.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = "FlexFi",
                        tint = FlexFiWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "FlexFi",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = FlexFiWhite,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Social Financial Intelligence",
                fontSize = 14.sp,
                color = FlexFiWhite.copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(48.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FlexFiWhite.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FlexFiWhite)
                )
            }
        }

        // Footer
        Text(
            text = "v2.4.0 • Secured by FlexProtocol",
            fontSize = 11.sp,
            color = FlexFiWhite.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

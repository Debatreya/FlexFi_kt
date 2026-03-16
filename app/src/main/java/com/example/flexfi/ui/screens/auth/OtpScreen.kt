package com.example.flexfi.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.FlexFiNumericKeypad
import com.example.flexfi.ui.components.FlexFiPrimaryButton
import com.example.flexfi.ui.theme.*

@Composable
fun OtpScreen(
    viewModel: AuthViewModel,
    onVerified: (AuthState) -> Unit,
    onBack: () -> Unit = {}
) {
    var digits by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    val authState by viewModel.authState.collectAsState()
    var timer by remember { mutableIntStateOf(59) }

    LaunchedEffect(authState) {
        if (authState is AuthState.UserExists || authState is AuthState.NewUser) {
            onVerified(authState)
        }
    }

    LaunchedEffect(timer) {
        if (timer > 0) {
            kotlinx.coroutines.delay(1000)
            timer--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlexFiWhite)
            .padding(20.dp)
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FlexFiDarkText)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Verify your phone",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = FlexFiDarkText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter the 6-digit code sent to your phone",
            fontSize = 14.sp,
            color = FlexFiBodyText
        )

        Spacer(Modifier.height(32.dp))

        // OTP boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            digits.forEachIndexed { index, digit ->
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FlexFiGreySurface)
                        .border(
                            width = 1.5.dp,
                            color = if (digit.isNotEmpty()) FlexFiBlue else FlexFiGreyBorder,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiDarkText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Resend timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = FlexFiBodyText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            if (timer > 0) {
                Text(
                    text = "Resend in 00:${"%02d".format(timer)}",
                    fontSize = 13.sp,
                    color = FlexFiBodyText,
                    fontWeight = FontWeight.Normal
                )
            } else {
                TextButton(
                    onClick = {
                        timer = 59
                        viewModel.resendOtp()
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Resend Code",
                        fontSize = 13.sp,
                        color = FlexFiBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (authState is AuthState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = FlexFiRed,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))

        FlexFiPrimaryButton(
            text = "Verify",
            onClick = { viewModel.verifyOtp(digits.joinToString("")) },
            isLoading = authState is AuthState.Loading,
            enabled = digits.all { it.isNotEmpty() },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = FlexFiWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        Spacer(Modifier.weight(1f))

        // Custom keypad
        FlexFiNumericKeypad(
            onKeyPress = { key ->
                val firstEmpty = digits.indexOfFirst { it.isEmpty() }
                if (firstEmpty != -1) {
                    digits = digits.toMutableList().also { it[firstEmpty] = key }
                }
            },
            onDelete = {
                val lastFilled = digits.indexOfLast { it.isNotEmpty() }
                if (lastFilled != -1) {
                    digits = digits.toMutableList().also { it[lastFilled] = "" }
                }
            },
            showDecimal = false
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Having trouble? Contact Support",
            fontSize = 12.sp,
            color = FlexFiBlue,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
    }
}

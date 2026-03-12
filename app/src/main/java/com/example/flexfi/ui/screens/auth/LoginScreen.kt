package com.example.flexfi.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.FlexFiPrimaryButton
import com.example.flexfi.ui.components.FlexFiTextField
import com.example.flexfi.ui.theme.*

import android.app.Activity
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onOtpSent: () -> Unit
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.CodeSent) {
            onOtpSent()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlexFiGreySurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(FlexFiGradients.splash),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FlexFiWhite.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "FlexFi",
                                tint = FlexFiWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "FlexFi",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiWhite
                    )
                }
            }

            // Form card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome Back",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiDarkText
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Sign in to continue tracking your expenses",
                        fontSize = 14.sp,
                        color = FlexFiBodyText
                    )

                    Spacer(Modifier.height(28.dp))

                    // Country code + phone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = countryCode,
                            onValueChange = { countryCode = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = FlexFiGreyBorder,
                                unfocusedContainerColor = FlexFiGreySurface
                            )
                        )
                        FlexFiTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "Phone Number",
                            leadingIcon = Icons.Default.Phone,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    FlexFiPrimaryButton(
                        text = "Continue",
                        onClick = { viewModel.sendOtp("$countryCode$phone", context as Activity) },
                        isLoading = authState is AuthState.Loading,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = FlexFiWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    // OR divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = FlexFiGreyBorder)
                        Text(
                            " OR ",
                            fontSize = 12.sp,
                            color = FlexFiLightText,
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = FlexFiGreyBorder)
                    }

                    Spacer(Modifier.height(20.dp))

                    // Google sign in
                    OutlinedButton(
                        onClick = { /* TODO: Google sign in */ },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder(true)
                    ) {
                        Text("Continue with Google", fontWeight = FontWeight.Medium)
                    }

                    if (authState is AuthState.Error) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = FlexFiRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Page dots
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FlexFiBlue)
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FlexFiGreyBorder)
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FlexFiGreyBorder)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "By continuing, you agree to our Terms & Privacy Policy",
                fontSize = 11.sp,
                color = FlexFiLightText,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

package com.example.flexfi.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.FlexFiPrimaryButton
import com.example.flexfi.ui.components.FlexFiTextField
import com.example.flexfi.ui.theme.*

@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.UserExists) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlexFiGreySurface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Back */ }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FlexFiDarkText)
            }
            Text(
                text = "Set Up Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FlexFiDarkText
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(FlexFiLightBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = FlexFiBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = FlexFiBlue,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Edit Photo",
                            tint = FlexFiWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Customize your look", fontSize = 14.sp, color = FlexFiBodyText)

            Spacer(Modifier.height(32.dp))

            // Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "PERSONAL DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiBodyText,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    FlexFiTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(Modifier.height(12.dp))
                    FlexFiTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email (Optional)",
                        leadingIcon = Icons.Default.Email
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your email will be used for account recovery",
                        fontSize = 12.sp,
                        color = FlexFiLightText
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            FlexFiPrimaryButton(
                text = "Continue",
                onClick = { viewModel.completeProfile(name, email) },
                isLoading = authState is AuthState.Loading,
                enabled = name.isNotBlank(),
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = FlexFiWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            Spacer(Modifier.height(24.dp))

            // Page dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(FlexFiGreyBorder))
                Box(Modifier.size(8.dp).clip(CircleShape).background(FlexFiGreyBorder))
                Box(Modifier.size(8.dp).clip(CircleShape).background(FlexFiBlue))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

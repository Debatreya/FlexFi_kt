package com.example.flexfi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.flexfi.data.local.FlexFiDatabase
import com.example.flexfi.data.local.entities.UserEntity
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.remote.FirestoreUserService
import com.example.flexfi.data.repository.UserRepository
import com.example.flexfi.ui.screens.auth.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual dependency injection for Phase 2
        val db = Room.databaseBuilder(
            applicationContext,
            FlexFiDatabase::class.java,
            "flexfi_db"
        ).fallbackToDestructiveMigration().build()

        val firestoreService = FirestoreUserService()
        val authService = FirebaseAuthService()
        val userRepository = UserRepository(db.userDao(), firestoreService)

        // Sign out user to force OTP login for testing
        authService.signOut()

        setContent {
            FlexFiApp(authService, firestoreService, userRepository)
        }
    }
}

@Composable
fun FlexFiApp(
    authService: FirebaseAuthService,
    firestoreService: FirestoreUserService,
    userRepository: UserRepository
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authService, firestoreService, userRepository)
    )

    // Check initial destination
    val startDestination = if (authService.getCurrentUser() != null) "home" else "login"

    MaterialTheme {
        Surface {
            NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(
                        viewModel = authViewModel,
                        onCodeSent = { navController.navigate("otp") }
                    )
                }
                composable("otp") {
                    OtpScreen(
                        viewModel = authViewModel,
                        onVerified = { state ->
                            if (state is AuthState.NewUser) {
                                navController.navigate("profile_setup")
                            } else {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable("profile_setup") {
                    ProfileSetupScreen(
                        viewModel = authViewModel,
                        onComplete = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(authService, onLogout = {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun HomeScreen(authService: FirebaseAuthService, onLogout: () -> Unit) {
    Surface {
        androidx.compose.foundation.layout.Column {
            Text("Welcome to FlexFi Home! 🚀")
            Button(onClick = {
                authService.signOut()
                onLogout()
            }) {
                Text("Logout")
            }
        }
    }
}

// Simple Factory for AuthViewModel
class AuthViewModelFactory(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreUserService,
    private val userRepository: UserRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(authService, firestoreService, userRepository) as T
    }
}

package com.example.flexfi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.flexfi.data.local.FlexFiDatabase
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.remote.FirestoreUserService
import com.example.flexfi.data.repository.ContactRepository
import com.example.flexfi.data.repository.UserRepository
import com.example.flexfi.ui.screens.auth.*
import com.example.flexfi.ui.screens.contacts.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual dependency injection
        val db = Room.databaseBuilder(
            applicationContext,
            FlexFiDatabase::class.java,
            "flexfi_db"
        ).fallbackToDestructiveMigration().build()

        val firestoreService = FirestoreUserService()
        val authService = FirebaseAuthService()
        val userRepository = UserRepository(db.userDao(), firestoreService)
        val contactRepository = ContactRepository(db.contactDao(), firestoreService)

        setContent {
            FlexFiApp(authService, firestoreService, userRepository, contactRepository)
        }
    }
}

@Composable
fun FlexFiApp(
    authService: FirebaseAuthService,
    firestoreService: FirestoreUserService,
    userRepository: UserRepository,
    contactRepository: ContactRepository
) {
    val navController = rememberNavController()
    
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authService, firestoreService, userRepository)
    )
    
    val contactViewModel: ContactViewModel = viewModel(
        factory = ContactViewModelFactory(contactRepository, authService)
    )

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
                    HomeScreen(
                        authService = authService,
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToContacts = {
                            navController.navigate("contacts")
                        }
                    )
                }
                composable("contacts") {
                    ContactsScreen(
                        viewModel = contactViewModel,
                        onAddContactClick = { navController.navigate("add_contact") }
                    )
                }
                composable("add_contact") {
                    AddContactScreen(
                        viewModel = contactViewModel,
                        onContactAdded = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authService: FirebaseAuthService, 
    onLogout: () -> Unit,
    onNavigateToContacts: () -> Unit
) {
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("FlexFi Home 🚀") }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNavigateToContacts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Contacts")
            }

            Button(
                onClick = {
                    authService.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

// Factories for ViewModels
class AuthViewModelFactory(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreUserService,
    private val userRepository: UserRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(authService, firestoreService, userRepository) as T
    }
}

class ContactViewModelFactory(
    private val contactRepository: ContactRepository,
    private val authService: FirebaseAuthService
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ContactViewModel(contactRepository, authService) as T
    }
}

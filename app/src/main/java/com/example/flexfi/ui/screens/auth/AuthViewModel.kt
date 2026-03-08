package com.example.flexfi.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfi.data.remote.FirebaseAuthService
import com.example.flexfi.data.remote.FirestoreUserService
import com.example.flexfi.data.remote.firestoreModels.UserDoc
import com.example.flexfi.data.repository.UserRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class CodeSent(val phoneNumber: String) : AuthState()
    object OtpVerified : AuthState()
    object UserExists : AuthState()
    object NewUser : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreUserService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var verificationId: String? = null

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _authState.value = AuthState.Error(e.message ?: "Verification failed")
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    _authState.value = AuthState.CodeSent(phoneNumber)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otp: String) {
        _authState.value = AuthState.Loading
        val id = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(id, otp)
        com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserStatus(task.result?.user?.uid ?: "")
                } else {
                    _authState.value = AuthState.Error("Invalid OTP")
                }
            }
    }

    private fun checkUserStatus(uid: String) {
        viewModelScope.launch {
            val userDoc = firestoreService.getUser(uid)
            if (userDoc != null) {
                userRepository.syncUser(uid)
                _authState.value = AuthState.UserExists
            } else {
                _authState.value = AuthState.NewUser
            }
        }
    }

    fun completeProfile(name: String, email: String) {
        val currentUser = authService.getCurrentUser() ?: return
        val uid = currentUser.uid
        val phone = currentUser.phoneNumber ?: ""
        
        viewModelScope.launch {
            val userDoc = UserDoc(
                id = uid,
                name = name,
                phone = phone,
                email = email,
                joinedAt = System.currentTimeMillis()
            )
            firestoreService.createUser(userDoc)
            userRepository.syncUser(uid)
            _authState.value = AuthState.UserExists
        }
    }
}

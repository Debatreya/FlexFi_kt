package com.example.flexfi.data.remote

import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthService {
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser() = auth.currentUser

    fun signOut() {
        auth.signOut()
    }
}

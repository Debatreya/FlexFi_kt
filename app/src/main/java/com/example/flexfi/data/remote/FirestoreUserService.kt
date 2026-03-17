package com.example.flexfi.data.remote

import android.util.Log
import com.example.flexfi.data.remote.firestoreModels.UserDoc
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreUserService {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getUser(uid: String): UserDoc? {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            snapshot.toObject(UserDoc::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreUserService", "Error getting user", e)
            null
        }
    }

    suspend fun findUserByPhone(phone: String): UserDoc? {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("phone", phone)
                .limit(1)
                .get()
                .await()
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[0].toObject(UserDoc::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirestoreUserService", "Error finding user by phone", e)
            null
        }
    }

    suspend fun createUser(user: UserDoc) {
        try {
            Log.d("FirestoreUserService", "Creating user in Firestore: ${user.id}")
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
            Log.d("FirestoreUserService", "User created successfully in Firestore")
        } catch (e: Exception) {
            Log.e("FirestoreUserService", "Error creating user", e)
            throw e
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, email: String?) {
        try {
            val payload = mutableMapOf<String, Any>(
                "name" to name
            )
            if (email != null) {
                payload["email"] = email
            }

            firestore.collection("users")
                .document(uid)
                .set(payload, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreUserService", "Error updating user profile", e)
            throw e
        }
    }
}

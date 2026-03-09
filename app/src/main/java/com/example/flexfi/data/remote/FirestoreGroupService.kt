package com.example.flexfi.data.remote

import android.util.Log
import com.example.flexfi.data.remote.firestoreModels.GroupDoc
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreGroupService {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createGroup(group: GroupDoc) {
        try {
            firestore.collection("groups")
                .document(group.id)
                .set(group)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreGroupService", "Error creating group", e)
            throw e
        }
    }

    suspend fun getGroupsForPhone(phone: String): List<GroupDoc> {
        return try {
            val snapshot = firestore.collection("groups")
                .whereArrayContains("memberPhones", phone)
                .get()
                .await()
            snapshot.toObjects(GroupDoc::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreGroupService", "Error getting groups", e)
            emptyList()
        }
    }

    suspend fun deleteGroup(groupId: String) {
        try {
            firestore.collection("groups")
                .document(groupId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreGroupService", "Error deleting group", e)
        }
    }
}

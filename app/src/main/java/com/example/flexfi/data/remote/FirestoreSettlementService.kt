package com.example.flexfi.data.remote

import android.util.Log
import com.example.flexfi.data.remote.firestoreModels.SettlementDoc
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSettlementService {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("settlements")

    suspend fun createSettlement(settlement: SettlementDoc) {
        try {
            collection.document(settlement.id).set(settlement).await()
        } catch (e: Exception) {
            Log.e("FirestoreSettlementService", "Error creating settlement", e)
        }
    }

    suspend fun updateSettlementStatus(id: String, status: String) {
        try {
            collection.document(id).update("status", status).await()
        } catch (e: Exception) {
            Log.e("FirestoreSettlementService", "Error updating settlement", e)
        }
    }

    suspend fun getSettlementsForUser(phone: String): List<SettlementDoc> {
        return try {
            val fromSnapshot = collection.whereEqualTo("fromPhone", phone).get().await()
            val toSnapshot = collection.whereEqualTo("toPhone", phone).get().await()
            
            val fromDocs = fromSnapshot.toObjects(SettlementDoc::class.java)
            val toDocs = toSnapshot.toObjects(SettlementDoc::class.java)
            
            (fromDocs + toDocs).distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("FirestoreSettlementService", "Error getting settlements", e)
            emptyList()
        }
    }
}

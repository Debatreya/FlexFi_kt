package com.example.flexfi.data.remote

import android.util.Log
import com.example.flexfi.data.remote.firestoreModels.ExpenseDoc
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreExpenseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("expenses")

    suspend fun createExpense(expense: ExpenseDoc) {
        try {
            collection.document(expense.id).set(expense).await()
        } catch (e: Exception) {
            Log.e("FirestoreExpenseService", "Error creating expense", e)
            throw e
        }
    }

    suspend fun getExpensesForGroup(groupId: String): List<ExpenseDoc> {
        return try {
            val snapshot = collection
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            snapshot.toObjects(ExpenseDoc::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreExpenseService", "Error getting expenses", e)
            emptyList()
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        try {
            collection.document(expenseId).delete().await()
        } catch (e: Exception) {
            Log.e("FirestoreExpenseService", "Error deleting expense", e)
        }
    }

    suspend fun deleteExpensesForGroup(groupId: String) {
        try {
            val snapshot = collection
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("FirestoreExpenseService", "Error batch deleting expenses", e)
        }
    }
}

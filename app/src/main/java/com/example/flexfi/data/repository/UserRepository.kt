package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.UserDao
import com.example.flexfi.data.local.entities.UserEntity
import com.example.flexfi.data.remote.FirestoreUserService
import com.example.flexfi.data.remote.firestoreModels.UserDoc // Added import
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val firestoreService: FirestoreUserService
) {
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    
    fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUserFlow()

    suspend fun getCurrentUserOnce(): UserEntity? = userDao.getCurrentUserFlowOnce()

    suspend fun getUser(id: String) = userDao.getUser(id)

    suspend fun syncUser(uid: String) {
        val firestoreUser = firestoreService.getUser(uid)
        if (firestoreUser != null) {
            val entity = UserEntity(
                id = firestoreUser.id,
                name = firestoreUser.name,
                phone = firestoreUser.phone,
                email = firestoreUser.email,
                joinedAt = firestoreUser.joinedAt,
                streakCount = firestoreUser.streakCount,
                totalExpense = firestoreUser.totalExpense
            )
            userDao.insertUser(entity)
        }
    }

    suspend fun uploadUser(user: UserEntity) {
        val doc = UserDoc(
            id = user.id,
            name = user.name,
            phone = user.phone,
            email = user.email,
            joinedAt = user.joinedAt,
            streakCount = user.streakCount,
            totalExpense = user.totalExpense
        )
        firestoreService.createUser(doc)
    }

    suspend fun updateCurrentUserProfile(name: String, email: String?) {
        val current = userDao.getCurrentUserFlowOnce() ?: return
        val normalizedEmail = email?.trim()?.takeIf { it.isNotBlank() }

        val updated = current.copy(
            name = name.trim(),
            email = normalizedEmail
        )
        userDao.insertUser(updated)
        firestoreService.updateUserProfile(current.id, updated.name, updated.email)
    }
}
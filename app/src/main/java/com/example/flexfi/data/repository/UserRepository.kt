package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.UserDao
import com.example.flexfi.data.local.entities.UserEntity

class UserRepository(
    private val userDao: UserDao
) {
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun getUser(id: String) = userDao.getUser(id)
}

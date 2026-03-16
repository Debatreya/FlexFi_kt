package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.AppSettingsDao
import com.example.flexfi.data.local.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsRepository(
    private val appSettingsDao: AppSettingsDao
) {

    fun getSettings(): Flow<AppSettingsEntity> =
        appSettingsDao.getSettings().map { it ?: AppSettingsEntity() }

    suspend fun getSettingsOnce(): AppSettingsEntity =
        appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()

    suspend fun saveSettings(settings: AppSettingsEntity) {
        appSettingsDao.updateSettings(settings)
    }

    suspend fun ensureDefaults() {
        val existing = appSettingsDao.getSettingsOnce()
        if (existing == null) {
            appSettingsDao.updateSettings(AppSettingsEntity())
        }
    }
}

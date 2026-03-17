package com.example.flexfi.data.repository

import android.content.Context
import com.example.flexfi.data.local.dao.AppSettingsDao
import com.example.flexfi.data.local.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsRepository(
    private val appSettingsDao: AppSettingsDao,
    context: Context? = null
) {

    private val prefs = context?.getSharedPreferences("flexfi_balance_meta", Context.MODE_PRIVATE)
    private val keyOpeningBalance = "opening_balance_base"
    private val keyBalanceAnchor = "balance_anchor_millis"

    fun getSettings(): Flow<AppSettingsEntity> =
        appSettingsDao.getSettings().map { it ?: AppSettingsEntity() }

    suspend fun getSettingsOnce(): AppSettingsEntity =
        appSettingsDao.getSettingsOnce() ?: AppSettingsEntity()

    suspend fun saveSettings(settings: AppSettingsEntity) {
        appSettingsDao.updateSettings(settings)
    }

    fun ensureBalanceAnchor(currentBalanceBase: Double) {
        val p = prefs ?: return
        val editor = p.edit()
        if (!p.contains(keyOpeningBalance)) {
            editor.putLong(keyOpeningBalance, java.lang.Double.doubleToRawLongBits(currentBalanceBase))
        }
        if (!p.contains(keyBalanceAnchor)) {
            editor.putLong(keyBalanceAnchor, 0L)
        }
        editor.apply()
    }

    fun setBalanceAnchor(currentBalanceBase: Double, anchorMillis: Long = System.currentTimeMillis()) {
        val p = prefs ?: return
        p.edit()
            .putLong(keyOpeningBalance, java.lang.Double.doubleToRawLongBits(currentBalanceBase))
            .putLong(keyBalanceAnchor, anchorMillis)
            .apply()
    }

    fun getOpeningBalanceBase(fallback: Double): Double {
        val p = prefs ?: return fallback
        if (!p.contains(keyOpeningBalance)) return fallback
        val bits = p.getLong(keyOpeningBalance, java.lang.Double.doubleToRawLongBits(fallback))
        return java.lang.Double.longBitsToDouble(bits)
    }

    fun getBalanceAnchorMillis(): Long = prefs?.getLong(keyBalanceAnchor, 0L) ?: 0L

    suspend fun adjustCurrentBankBalance(deltaBase: Double) {
        val current = getSettingsOnce()
        saveSettings(current.copy(currentBankBalance = current.currentBankBalance + deltaBase))
    }

    suspend fun ensureDefaults() {
        val existing = appSettingsDao.getSettingsOnce()
        if (existing == null) {
            appSettingsDao.updateSettings(AppSettingsEntity())
        }
    }
}

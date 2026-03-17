package com.example.flexfi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flexfi.data.local.entities.SettlementRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SettlementRecordEntity)

    @Query("SELECT * FROM settlement_records WHERE id = :id LIMIT 1")
    suspend fun getSettlementById(id: String): SettlementRecordEntity?

    @Query("UPDATE settlement_records SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM settlement_records WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getSettlementsForGroup(groupId: String): Flow<List<SettlementRecordEntity>>

    @Query("SELECT * FROM settlement_records WHERE groupId = :groupId ORDER BY createdAt DESC")
    suspend fun getSettlementsForGroupOnce(groupId: String): List<SettlementRecordEntity>

    @Query("SELECT * FROM settlement_records WHERE fromPhone = :phone OR toPhone = :phone ORDER BY createdAt DESC")
    fun getSettlementsForUser(phone: String): Flow<List<SettlementRecordEntity>>

    @Query("SELECT * FROM settlement_records WHERE fromPhone = :phone OR toPhone = :phone ORDER BY createdAt DESC")
    suspend fun getSettlementsForUserOnce(phone: String): List<SettlementRecordEntity>

    @Query("SELECT * FROM settlement_records ORDER BY createdAt DESC")
    fun getAllSettlements(): Flow<List<SettlementRecordEntity>>

    @Query("DELETE FROM settlement_records")
    suspend fun deleteAll()

    @Query("DELETE FROM settlement_records WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)
}

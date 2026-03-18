package com.example.flexfi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flexfi.data.local.entities.AIInsightEntity

@Dao
interface AIInsightDao {

    @Query("SELECT * FROM ai_insights WHERE dataHash = :dataHash AND cacheType = :cacheType LIMIT 1")
    suspend fun getByDataHashAndType(dataHash: Long, cacheType: String): AIInsightEntity?

    @Query("SELECT * FROM ai_insights WHERE cacheType = :cacheType ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestByType(cacheType: String): AIInsightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AIInsightEntity): Long

    @Query("DELETE FROM ai_insights WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long): Int

    @Query("DELETE FROM ai_insights")
    suspend fun deleteAll()
}

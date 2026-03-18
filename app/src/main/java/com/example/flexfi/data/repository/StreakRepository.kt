package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.StreakDao
import com.example.flexfi.data.local.entities.StreakEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class StreakRepository(private val streakDao: StreakDao) {

    fun getStreak(userId: String): Flow<StreakEntity?> = streakDao.getStreakFlow(userId)

    suspend fun getStreakSync(userId: String): StreakEntity? = streakDao.getStreakSync(userId)

    /**
     * Updates the user's streak based on the current date.
     * Call this after a successful expense creation.
     */
    suspend fun updateStreak(userId: String) {
        val currentStreakEntity = streakDao.getStreakSync(userId)
        val now = System.currentTimeMillis()

        if (currentStreakEntity == null) {
            // First time logging an expense
            streakDao.upsert(
                StreakEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    currentStreak = 1,
                    longestStreak = 1,
                    lastUpdated = now
                )
            )
            return
        }

        // Determine if lastUpdated was today, yesterday, or before
        val todayStart = getStartOfDay(now)
        val yesterdayStart = todayStart - (24 * 60 * 60 * 1000)
        
        val lastUpdatedStart = getStartOfDay(currentStreakEntity.lastUpdated)

        var newCurrentStreak = currentStreakEntity.currentStreak
        var newLongestStreak = currentStreakEntity.longestStreak

        if (lastUpdatedStart == todayStart) {
            // Already logged today, do nothing
            return
        } else if (lastUpdatedStart == yesterdayStart) {
            // Logged yesterday, increment streak
            newCurrentStreak += 1
            if (newCurrentStreak > newLongestStreak) {
                newLongestStreak = newCurrentStreak
            }
        } else {
            // Missed a day or more, reset streak
            newCurrentStreak = 1
        }

        streakDao.upsert(
            currentStreakEntity.copy(
                currentStreak = newCurrentStreak,
                longestStreak = newLongestStreak,
                lastUpdated = now
            )
        )
    }

    private fun getStartOfDay(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}

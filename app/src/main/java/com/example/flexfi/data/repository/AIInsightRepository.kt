package com.example.flexfi.data.repository

import com.example.flexfi.flexcard.FlexCardLLMResponse
import com.example.flexfi.data.local.dao.AIInsightDao
import com.example.flexfi.data.local.entities.AIInsightEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AIInsightRepository(private val aiInsightDao: AIInsightDao) {

    private companion object {
        const val CACHE_TYPE_INSIGHTS = "INSIGHTS"
        const val CACHE_TYPE_EXPLAIN = "EXPLAIN"
        const val CACHE_TYPE_FLEX_CARD = "FLEX_CARD"
    }

    /**
     * Retrieve cached insights if valid (not expired) and data hash matches
     * @param dataHash Hash of the financial data used to generate insights
     * @return Cached insights or null if cache miss or expired
     */
    suspend fun getInsightIfValid(dataHash: Long): List<String>? = withContext(Dispatchers.IO) {
        val cached = aiInsightDao.getByDataHashAndType(dataHash, CACHE_TYPE_INSIGHTS) ?: return@withContext null
        
        // Check if cache is still valid (not expired)
        if (cached.expiresAt > System.currentTimeMillis()) {
            return@withContext deserializeInsights(cached.content)
        }
        
        // Cache expired, delete it
        aiInsightDao.deleteExpired(System.currentTimeMillis())
        null
    }

    suspend fun getExplanationIfValid(dataHash: Long): String? = withContext(Dispatchers.IO) {
        val cached = aiInsightDao.getByDataHashAndType(dataHash, CACHE_TYPE_EXPLAIN) ?: return@withContext null

        if (cached.expiresAt > System.currentTimeMillis()) {
            return@withContext cached.content
        }

        aiInsightDao.deleteExpired(System.currentTimeMillis())
        null
    }

    /**
     * Cache generated insights to local database
     * @param insights List of 3 insights to cache
     * @param dataHash Hash of the financial data
     */
    suspend fun cacheInsights(insights: List<String>, dataHash: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expiresAt = now + (24 * 60 * 60 * 1000) // 24 hours from now
        
        val entity = AIInsightEntity(
            id = UUID.randomUUID().toString(),
            content = serializeInsights(insights),
            dataHash = dataHash,
            cacheType = CACHE_TYPE_INSIGHTS,
            generatedAt = now,
            expiresAt = expiresAt
        )
        
        aiInsightDao.insertOrUpdate(entity)
    }

    suspend fun cacheExplanation(explanation: String, dataHash: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expiresAt = now + (24 * 60 * 60 * 1000)

        val entity = AIInsightEntity(
            id = UUID.randomUUID().toString(),
            content = explanation,
            dataHash = dataHash,
            cacheType = CACHE_TYPE_EXPLAIN,
            generatedAt = now,
            expiresAt = expiresAt
        )

        aiInsightDao.insertOrUpdate(entity)
    }

    suspend fun getFlexCardIfValid(dataHash: Long, monthKey: String): FlexCardLLMResponse? = withContext(Dispatchers.IO) {
        val monthlyHash = monthlyHash(dataHash, monthKey)
        val cached = aiInsightDao.getByDataHashAndType(monthlyHash, CACHE_TYPE_FLEX_CARD) ?: return@withContext null

        if (cached.expiresAt > System.currentTimeMillis()) {
            return@withContext deserializeFlexCard(cached.content)
        }

        aiInsightDao.deleteExpired(System.currentTimeMillis())
        null
    }

    suspend fun cacheFlexCard(
        response: FlexCardLLMResponse,
        dataHash: Long,
        monthKey: String
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expiresAt = now + (31L * 24L * 60L * 60L * 1000L)
        val monthlyHash = monthlyHash(dataHash, monthKey)

        val entity = AIInsightEntity(
            id = UUID.randomUUID().toString(),
            content = serializeFlexCard(response),
            dataHash = monthlyHash,
            cacheType = CACHE_TYPE_FLEX_CARD,
            generatedAt = now,
            expiresAt = expiresAt
        )

        aiInsightDao.insertOrUpdate(entity)
    }

    /**
     * Clear all cached insights manually
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        aiInsightDao.deleteAll()
    }

    /**
     * Clear expired insights only
     */
    suspend fun clearExpiredCache() = withContext(Dispatchers.IO) {
        aiInsightDao.deleteExpired(System.currentTimeMillis())
    }

    /**
     * Get the most recently cached insights (regardless of expiry)
     */
    suspend fun getLatestCachedInsights(): List<String>? = withContext(Dispatchers.IO) {
        val cached = aiInsightDao.getLatestByType(CACHE_TYPE_INSIGHTS) ?: return@withContext null
        deserializeInsights(cached.content)
    }

    // Private serialization helpers
    private fun serializeInsights(insights: List<String>): String {
        val json = JSONArray()
        insights.forEach { json.put(it) }
        return json.toString()
    }

    private fun deserializeInsights(serialized: String): List<String> {
        return try {
            val json = JSONArray(serialized)
            buildList {
                for (i in 0 until json.length()) {
                    add(json.optString(i))
                }
            }
        } catch (_: Exception) {
            // Backward compatibility with earlier "|" serialization.
            serialized.split("|")
        }
    }

    private fun serializeFlexCard(response: FlexCardLLMResponse): String {
        return JSONObject().apply {
            put("highlights", JSONArray().apply {
                response.highlights.forEach { put(it) }
            })
            put("improvement", response.improvement)
            put("tagline", response.tagline)
        }.toString()
    }

    private fun deserializeFlexCard(serialized: String): FlexCardLLMResponse? {
        return runCatching {
            val root = JSONObject(serialized)
            val highlightsArray = root.optJSONArray("highlights") ?: return null
            val highlights = buildList {
                for (i in 0 until highlightsArray.length()) {
                    add(highlightsArray.optString(i).trim())
                }
            }

            FlexCardLLMResponse(
                highlights = highlights,
                improvement = root.optString("improvement").trim(),
                tagline = root.optString("tagline").trim()
            )
        }.getOrNull()
    }

    private fun monthlyHash(dataHash: Long, monthKey: String): Long {
        return "$dataHash|$monthKey".hashCode().toLong()
    }
}

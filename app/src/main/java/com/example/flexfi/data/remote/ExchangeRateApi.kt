package com.example.flexfi.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Lightweight client for frankfurter.app exchange rates.
 * - Free
 * - No API key
 * - Supports historical daily rates
 */
class ExchangeRateApi {

    private val inMemoryRateCache = mutableMapOf<String, Double>()

    suspend fun getRate(
        fromCurrency: String,
        toCurrency: String,
        dateMillis: Long
    ): Double = withContext(Dispatchers.IO) {
        val from = fromCurrency.uppercase(Locale.US)
        val to = toCurrency.uppercase(Locale.US)

        if (from == to) return@withContext 1.0

        val day = utcDay(dateMillis)
        val cacheKey = "$day|$from|$to"
        inMemoryRateCache[cacheKey]?.let { return@withContext it }

        val endpoint = "https://api.frankfurter.app/$day?from=$from&to=$to"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext fallbackRate(from, to)
            }

            val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val json = JSONObject(body)
            val rates = json.optJSONObject("rates") ?: return@withContext fallbackRate(from, to)
            val rate = rates.optDouble(to, -1.0)
            if (rate <= 0.0) return@withContext fallbackRate(from, to)

            inMemoryRateCache[cacheKey] = rate
            rate
        } catch (_: Exception) {
            fallbackRate(from, to)
        } finally {
            connection.disconnect()
        }
    }

    private fun utcDay(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    /**
     * Conservative fallback if network is unavailable.
     */
    private fun fallbackRate(from: String, to: String): Double {
        val usdToInr = 83.0
        return when {
            from == "USD" && to == "INR" -> usdToInr
            from == "INR" && to == "USD" -> 1.0 / usdToInr
            from == "USD" && to == "JPY" -> 150.0
            from == "JPY" && to == "USD" -> 1.0 / 150.0
            from == "USD" && to == "EUR" -> 0.92
            from == "EUR" && to == "USD" -> 1.0 / 0.92
            from == "USD" && to == "GBP" -> 0.78
            from == "GBP" && to == "USD" -> 1.0 / 0.78
            else -> 1.0
        }
    }
}

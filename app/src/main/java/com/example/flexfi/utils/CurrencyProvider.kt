package com.example.flexfi.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Currency
import java.util.Locale

/**
 * Manages app-wide display currency.
 *
 * All persisted financial values are stored in USD (base currency).
 * This provider converts USD values into the currently selected display currency.
 * Must call [init] once from Application/Activity before use.
 */
object CurrencyProvider {
    private const val PREFS_NAME = "flexfi_prefs"
    private const val KEY_CURRENCY_CODE = "display_currency_code"
    private const val KEY_RATE_FROM_BASE = "display_rate_from_usd"
    private const val BASE_CURRENCY = "USD"
    private const val DEFAULT_CODE = "INR"
    private const val DEFAULT_RATE_FROM_BASE = 83.0

    private lateinit var prefs: SharedPreferences

    val baseCurrency: String
        get() = BASE_CURRENCY

    val displayCurrencyCode: String
        get() = if (::prefs.isInitialized) {
            prefs.getString(KEY_CURRENCY_CODE, DEFAULT_CODE) ?: DEFAULT_CODE
        } else {
            DEFAULT_CODE
        }

    val displayRateFromBase: Double
        get() = if (::prefs.isInitialized) {
            prefs.getFloat(KEY_RATE_FROM_BASE, DEFAULT_RATE_FROM_BASE.toFloat()).toDouble()
        } else {
            DEFAULT_RATE_FROM_BASE
        }

    val symbol: String
        get() = try {
            Currency.getInstance(displayCurrencyCode).symbol
        } catch (_: Exception) {
            displayCurrencyCode
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setDisplayCurrency(code: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_CURRENCY_CODE, code.uppercase(Locale.US)).apply()
        }
    }

    fun setDisplayRateFromBase(rate: Double) {
        if (::prefs.isInitialized && rate > 0.0) {
            prefs.edit().putFloat(KEY_RATE_FROM_BASE, rate.toFloat()).apply()
        }
    }

    fun convertFromBase(baseAmount: Double): Double {
        return baseAmount * displayRateFromBase
    }

    fun convertToBase(amountInDisplayCurrency: Double): Double {
        val rate = displayRateFromBase
        if (rate <= 0.0) return amountInDisplayCurrency
        return amountInDisplayCurrency / rate
    }

    fun formatAmount(baseAmount: Double): String {
        val amount = kotlin.math.abs(convertFromBase(baseAmount))
        return "$symbol${"%.2f".format(amount)}"
    }

    fun formatSigned(baseAmount: Double): String {
        val converted = convertFromBase(baseAmount)
        val prefix = if (converted > 0) "+" else if (converted < 0) "-" else ""
        return "$prefix$symbol${"%.2f".format(kotlin.math.abs(converted))}"
    }

    /**
     * Formats a transaction amount with minimal conversion noise.
     * If the transaction currency matches current display currency, use original entered amount.
     * Otherwise, fall back to formatting from base amount.
     */
    fun formatTransactionAmount(amount: Double, currency: String, baseAmount: Double): String {
        return if (currency.equals(displayCurrencyCode, ignoreCase = true)) {
            "$symbol${"%.2f".format(kotlin.math.abs(amount))}"
        } else {
            formatAmount(baseAmount)
        }
    }

    data class CurrencyOption(val code: String, val symbol: String, val displayLabel: String)

    val supportedCurrencies: List<CurrencyOption> = listOf(
        CurrencyOption("INR", "₹", "INR (₹)"),
        CurrencyOption("USD", "$", "USD ($)"),
        CurrencyOption("EUR", "€", "EUR (€)"),
        CurrencyOption("JPY", "¥", "JPY (¥)"),
        CurrencyOption("GBP", "£", "GBP (£)"),
        CurrencyOption("AUD", "A$", "AUD (A$)"),
        CurrencyOption("CAD", "C$", "CAD (C$)"),
        CurrencyOption("SGD", "S$", "SGD (S$)")
    )

    val supportedCurrencyCodes: List<String> get() = supportedCurrencies.map { it.code }

    fun getDisplayLabel(code: String): String =
        supportedCurrencies.find { it.code == code }?.displayLabel ?: code

    fun getSymbol(code: String): String =
        supportedCurrencies.find { it.code == code }?.symbol ?: code
}

package com.example.flexfi.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages currency preference ($ or ₹) throughout the app.
 * Must call [init] once from Application/Activity before use.
 */
object CurrencyProvider {
    private const val PREFS_NAME = "flexfi_prefs"
    private const val KEY_CURRENCY = "currency_symbol"
    private const val DEFAULT_SYMBOL = "$"

    private lateinit var prefs: SharedPreferences

    val symbol: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_CURRENCY, DEFAULT_SYMBOL) ?: DEFAULT_SYMBOL else DEFAULT_SYMBOL

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSymbol(symbol: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_CURRENCY, symbol).apply()
        }
    }

    fun formatAmount(amount: Double): String {
        return "$symbol${"%.2f".format(kotlin.math.abs(amount))}"
    }

    fun formatSigned(amount: Double): String {
        val prefix = if (amount > 0) "+" else if (amount < 0) "-" else ""
        return "$prefix$symbol${"%.2f".format(kotlin.math.abs(amount))}"
    }

    val availableSymbols = listOf("$", "₹")
}

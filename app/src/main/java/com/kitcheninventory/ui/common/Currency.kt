package com.kitcheninventory.ui.common

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A currency the user can pick. [symbol] is fixed so "$" never shows as "US$" on some phones. */
data class CurrencyOption(val code: String, val name: String, val symbol: String)

val currencyOptions = listOf(
    CurrencyOption("INR", "Indian rupee", "₹"),
    CurrencyOption("USD", "US dollar", "$"),
    CurrencyOption("EUR", "Euro", "€"),
    CurrencyOption("GBP", "British pound", "£"),
    CurrencyOption("AED", "UAE dirham", "AED "),
    CurrencyOption("SAR", "Saudi riyal", "SAR "),
    CurrencyOption("QAR", "Qatari riyal", "QAR "),
    CurrencyOption("CAD", "Canadian dollar", "CA$"),
    CurrencyOption("AUD", "Australian dollar", "A$"),
    CurrencyOption("NZD", "New Zealand dollar", "NZ$"),
    CurrencyOption("SGD", "Singapore dollar", "S$"),
    CurrencyOption("MYR", "Malaysian ringgit", "RM"),
    CurrencyOption("JPY", "Japanese yen", "¥"),
    CurrencyOption("CNY", "Chinese yuan", "CN¥"),
    CurrencyOption("PHP", "Philippine peso", "₱"),
    CurrencyOption("THB", "Thai baht", "฿"),
    CurrencyOption("LKR", "Sri Lankan rupee", "Rs "),
    CurrencyOption("NPR", "Nepalese rupee", "Rs "),
    CurrencyOption("PKR", "Pakistani rupee", "Rs "),
    CurrencyOption("BDT", "Bangladeshi taka", "৳"),
    CurrencyOption("ZAR", "South African rand", "R"),
    CurrencyOption("NGN", "Nigerian naira", "₦"),
    CurrencyOption("KES", "Kenyan shilling", "KSh "),
)

/**
 * The currency money is shown in. Null follows the phone's region. Backed by Compose state so every
 * screen redraws when it changes, and saved in shared preferences.
 */
object AppCurrency {
    private const val PREFS = "settings"
    private const val KEY = "currency"

    var selected: CurrencyOption? by mutableStateOf(null)
        private set

    fun load(context: Context) {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        selected = currencyOptions.firstOrNull { it.code == code }
    }

    fun select(context: Context, option: CurrencyOption?) {
        selected = option
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, option?.code).apply()
    }

    /** For tests: switch currency without saving it. */
    internal fun setForTest(option: CurrencyOption?) {
        selected = option
    }
}

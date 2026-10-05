package com.kitcheninventory.ui.common

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.abs
import kotlin.math.pow

private val quantityFormat = DecimalFormat("#,##0.###")

/** 2.5 -> "2.5", 3.0 -> "3". Tiny rounding leftovers show as "0", never "-0". */
fun formatQuantity(value: Double): String =
    if (abs(value) < 0.0005) "0" else quantityFormat.format(value)

/**
 * Money in the currency picked in settings, for example "₹1,250.50" or "¥1,250", using the phone's
 * digit grouping. With no currency picked, the phone's own currency format is used.
 */
fun formatMoney(value: Double): String {
    val option = AppCurrency.selected ?: return NumberFormat.getCurrencyInstance().format(value)
    val digits = Currency.getInstance(option.code).defaultFractionDigits.coerceAtLeast(0)
    val number = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    // Amounts that round to zero show as "0.00", never "-0.00".
    val sign = if (value < 0 && abs(value) >= 0.5 * 10.0.pow(-digits)) "-" else ""
    return sign + option.symbol + number.format(abs(value))
}

/** The symbol of the chosen currency (or the phone's), for example "₹" or "$". */
fun currencySymbol(): String =
    AppCurrency.selected?.symbol?.trim() ?: NumberFormat.getCurrencyInstance().currency?.symbol.orEmpty()

/**
 * Parses user input. Blank means zero. A comma is read as a decimal separator ("2,5"), unless the
 * text also has a dot, in which case commas are thousands separators ("1,250.50").
 */
fun parseNumber(text: String): Double? {
    val trimmed = text.trim().replace(" ", "")
    val cleaned = if ('.' in trimmed) trimmed.replace(",", "") else trimmed.replace(',', '.')
    if (cleaned.isEmpty()) return 0.0
    return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
}

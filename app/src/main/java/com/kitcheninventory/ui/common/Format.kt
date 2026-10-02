package com.kitcheninventory.ui.common

import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.abs

private val quantityFormat = DecimalFormat("#,##0.###")

/** 2.5 -> "2.5", 3.0 -> "3". Tiny rounding leftovers show as "0", never "-0". */
fun formatQuantity(value: Double): String =
    if (abs(value) < 0.0005) "0" else quantityFormat.format(value)

fun formatMoney(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

/** The symbol of the phone's local currency, for example "₹" or "$". */
fun currencySymbol(): String = NumberFormat.getCurrencyInstance().currency?.symbol.orEmpty()

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

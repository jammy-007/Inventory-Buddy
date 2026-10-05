package com.kitcheninventory

import com.kitcheninventory.ui.common.AppCurrency
import com.kitcheninventory.ui.common.currencyOptions
import com.kitcheninventory.ui.common.currencySymbol
import com.kitcheninventory.ui.common.formatMoney
import java.util.Currency
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CurrencyTest {
    private val original = Locale.getDefault()

    @Before fun useUsLocale() = Locale.setDefault(Locale.US)

    @After fun restore() {
        Locale.setDefault(original)
        AppCurrency.setForTest(null)
    }

    private fun pick(code: String) = AppCurrency.setForTest(currencyOptions.first { it.code == code })

    @Test fun formatsWithChosenSymbolAndDecimals() {
        pick("INR")
        assertEquals("₹1,250.50", formatMoney(1250.5))
        assertEquals("-₹3.00", formatMoney(-3.0))
        assertEquals("₹0.00", formatMoney(-0.001))
        assertEquals("₹", currencySymbol())

        pick("JPY")
        assertEquals("¥1,251", formatMoney(1250.6))

        pick("AED")
        assertEquals("AED 10.00", formatMoney(10.0))
        assertEquals("AED", currencySymbol())
    }

    @Test fun phoneDefaultWhenNothingPicked() {
        assertEquals("$5.00", formatMoney(5.0))
    }

    @Test fun everyOptionIsARealCurrency() {
        currencyOptions.forEach { Currency.getInstance(it.code) }
        assertEquals(currencyOptions.size, currencyOptions.map { it.code }.toSet().size)
    }
}

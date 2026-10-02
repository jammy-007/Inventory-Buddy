package com.kitcheninventory

import com.kitcheninventory.data.db.ItemEntity
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.isLowStock
import com.kitcheninventory.ui.common.formatQuantity
import com.kitcheninventory.ui.common.parseNumber
import com.kitcheninventory.ui.items.ItemForm
import com.kitcheninventory.ui.items.filterItems
import com.kitcheninventory.ui.items.toEntity
import com.kitcheninventory.ui.items.toForm
import com.kitcheninventory.ui.items.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemFormTest {
    private val valid = ItemForm(name = "Tomatoes", unitId = 1, unitCost = "40", parLevel = "5", openingStock = "12.5")

    @Test fun validFormHasNoErrors() {
        assertTrue(validate(valid, isNew = true, nameTaken = false).isEmpty)
    }

    @Test fun requiresNameAndUnit() {
        val errors = validate(ItemForm(), isNew = true, nameTaken = false)
        assertNotNull(errors.name)
        assertNotNull(errors.unit)
    }

    @Test fun rejectsDuplicateName() {
        assertNotNull(validate(valid, isNew = true, nameTaken = true).name)
    }

    @Test fun rejectsNegativeAndNonNumbers() {
        val errors = validate(valid.copy(unitCost = "-1", parLevel = "abc"), isNew = true, nameTaken = false)
        assertNotNull(errors.unitCost)
        assertNotNull(errors.parLevel)
    }

    @Test fun openingStockOnlyCheckedForNewItems() {
        assertNull(validate(valid.copy(openingStock = "x"), isNew = false, nameTaken = false).openingStock)
    }

    @Test fun parsesCommaDecimalsAndBlank() {
        assertEquals(2.5, parseNumber("2,5")!!, 0.0)
        assertEquals(0.0, parseNumber(" ")!!, 0.0)
        assertNull(parseNumber("NaN"))
        assertNull(parseNumber("Infinity"))
    }

    @Test fun parsesThousandsSeparators() {
        assertEquals(1250.5, parseNumber("1,250.50")!!, 0.0)
        assertEquals(1250.0, parseNumber("1 250")!!, 0.0)
    }

    @Test fun lowStockIgnoresRoundingLeftovers() {
        assertTrue(isLowStock(onHand = 0.1 + 0.2 + 4.7, parLevel = 5.0))
        assertFalse(isLowStock(onHand = 5.01, parLevel = 5.0))
        assertFalse(isLowStock(onHand = 0.0, parLevel = 0.0))
    }

    @Test fun quantitiesNeverShowMinusZero() {
        assertEquals("0", formatQuantity(0.1 + 0.2 - 0.3 - 1e-12))
        assertEquals("2.5", formatQuantity(2.5))
    }

    @Test fun editKeepsIdAndCreatedAt() {
        val existing = ItemEntity(id = 7, name = "Milk", unitId = 3, createdAt = 100)
        val updated = existing.toForm().copy(name = " Whole milk ", unitCost = "1.25").toEntity(existing)
        assertEquals(7, updated.id)
        assertEquals(100, updated.createdAt)
        assertEquals("Whole milk", updated.name)
        assertEquals(1.25, updated.unitCost, 0.0)
    }

    @Test fun formShowsWholeNumbersWithoutDecimals() {
        val form = ItemEntity(name = "Eggs", unitId = 1, unitCost = 3.0, parLevel = 2.5).toForm()
        assertEquals("3", form.unitCost)
        assertEquals("2.5", form.parLevel)
    }

    @Test fun filtersBySearchCategoryAndLowStock() {
        fun item(id: Long, name: String, cat: Long?, onHand: Double, par: Double, supplier: String? = null) =
            ItemWithStock(id, name, 1, "kg", cat, null, null, supplier, 0.0, par, onHand)
        val items = listOf(
            item(1, "Tomatoes", 1, onHand = 2.0, par = 5.0),
            item(2, "Onions", 1, onHand = 10.0, par = 5.0, supplier = "Fresh Farms"),
            item(3, "Milk", 2, onHand = 1.0, par = 0.0),
        )
        assertEquals(listOf(1L), filterItems(items, "tom", null, false).map { it.id })
        assertEquals(listOf(2L), filterItems(items, "fresh", null, false).map { it.id })
        assertEquals(listOf(1L, 2L), filterItems(items, "", 1, false).map { it.id })
        assertEquals(listOf(1L), filterItems(items, "", null, true).map { it.id })
        assertFalse(items[2].isLowStock) // no reorder level set means no alert
    }
}

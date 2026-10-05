package com.kitcheninventory

import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.DefaultData
import com.kitcheninventory.data.db.UnitEntity
import com.kitcheninventory.ui.items.ItemForm
import com.kitcheninventory.ui.items.quickItems
import com.kitcheninventory.ui.items.withQuickItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickItemsTest {
    private val units = DefaultData.units.mapIndexed { i, (name, abbr) -> UnitEntity(i + 1L, name, abbr) }
    private val categories = DefaultData.categories.mapIndexed { i, name -> CategoryEntity(i + 1L, name) }

    @Test fun everyQuickItemMatchesADefaultUnitAndCategory() {
        quickItems.forEach { item ->
            assertTrue(item.name, units.any { it.abbreviation == item.unit })
            assertTrue(item.name, categories.any { it.name == item.category })
        }
        assertEquals(quickItems.size, quickItems.map { it.name.lowercase() }.toSet().size)
    }

    @Test fun fillsNameUnitAndCategoryButKeepsCostAndStock() {
        val cabbage = quickItems.first { it.name == "Cabbage" }
        val form = ItemForm(unitCost = "30", openingStock = "4").withQuickItem(cabbage, units, categories)
        assertEquals("Cabbage", form.name)
        assertEquals("pc", units.first { it.id == form.unitId }.abbreviation)
        assertEquals("Produce", categories.first { it.id == form.categoryId }.name)
        assertEquals("30", form.unitCost)
        assertEquals("4", form.openingStock)
    }
}

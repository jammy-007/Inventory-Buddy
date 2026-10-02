package com.kitcheninventory

import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.MovementType
import com.kitcheninventory.data.db.MovementWithItem
import com.kitcheninventory.ui.history.HistoryFilter
import com.kitcheninventory.ui.history.filterMovements
import com.kitcheninventory.ui.history.groupByDay
import com.kitcheninventory.ui.stock.EntryKind
import com.kitcheninventory.ui.stock.buildCountAdjustments
import com.kitcheninventory.ui.stock.buildEntries
import com.kitcheninventory.ui.stock.countError
import com.kitcheninventory.ui.stock.entryError
import com.kitcheninventory.ui.stock.newlyLowStock
import com.kitcheninventory.ui.stock.timestampFor
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockEntriesTest {
    private fun item(id: Long, onHand: Double, par: Double = 0.0, cost: Double = 2.0) =
        ItemWithStock(id, "Item $id", 1, "kg", null, null, null, null, cost, par, onHand)

    private val items = listOf(item(1, onHand = 10.0, par = 4.0), item(2, onHand = 3.0), item(3, onHand = 0.0))

    @Test fun entryValidation() {
        assertNull(entryError(""))
        assertNull(entryError("2,5"))
        assertNotNull(entryError("0"))
        assertNotNull(entryError("-1"))
        assertNotNull(entryError("abc"))
    }

    @Test fun countAllowsZeroButNotNegative() {
        assertNull(countError("0"))
        assertNull(countError(""))
        assertNotNull(countError("-2"))
    }

    @Test fun usageAndWasteAreNegativeAndStockInIsPositive() {
        val quantities = mapOf(1L to "2.5", 2L to "", 3L to "1")
        val usage = buildEntries(EntryKind.USAGE, quantities, items, " lunch ", 42)
        assertEquals(listOf(-2.5, -1.0), usage.map { it.quantity })
        assertTrue(usage.all { it.type == MovementType.USAGE && it.note == "lunch" && it.timestamp == 42L })
        assertEquals(2.0, usage.first().unitCost, 0.0)

        val stockIn = buildEntries(EntryKind.STOCK_IN, quantities, items, "", 0)
        assertEquals(listOf(2.5, 1.0), stockIn.map { it.quantity })
        assertEquals(MovementType.WASTE, buildEntries(EntryKind.WASTE, quantities, items, "", 0).first().type)
    }

    @Test fun skipsInvalidAndUnknownItems() {
        val entries = buildEntries(EntryKind.USAGE, mapOf(1L to "x", 99L to "3", 2L to "1"), items, "", 0)
        assertEquals(listOf(2L), entries.map { it.itemId })
    }

    @Test fun countCreatesAdjustmentForDifferencesOnly() {
        val adjustments = buildCountAdjustments(mapOf(1L to "7", 2L to "3", 3L to "1.5"), items, 0)
        assertEquals(listOf(1L to -3.0, 3L to 1.5), adjustments.map { it.itemId to it.quantity })
        assertTrue(adjustments.all { it.type == MovementType.ADJUSTMENT })
    }

    @Test fun countIgnoresFloatingPointNoise() {
        val noisy = listOf(item(1, onHand = 0.1 + 0.2))
        assertTrue(buildCountAdjustments(mapOf(1L to "0.3"), noisy, 0).isEmpty())
    }

    @Test fun reportsItemsThatJustWentLow() {
        val usage = buildEntries(EntryKind.USAGE, mapOf(1L to "6", 2L to "3"), items, "", 0)
        // Item 1 drops from 10 to 4 (reorder at 4). Item 2 has no reorder level.
        assertEquals(listOf(1L), newlyLowStock(items, usage).map { it.id })

        val alreadyLow = listOf(item(1, onHand = 3.0, par = 4.0))
        val more = buildEntries(EntryKind.USAGE, mapOf(1L to "1"), alreadyLow, "", 0)
        assertTrue(newlyLowStock(alreadyLow, more).isEmpty())
    }

    @Test fun pastDatesKeepTheirDay() {
        val zone = ZoneId.of("Asia/Kolkata")
        val date = LocalDate.of(2026, 9, 20)
        val ts = timestampFor(date, zone)
        assertEquals(date, java.time.Instant.ofEpochMilli(ts).atZone(zone).toLocalDate())
    }

    private fun movement(id: Long, type: MovementType, name: String, day: Int, note: String = "") = MovementWithItem(
        id, 1, name, "kg", type, 1.0, 0.0, note,
        LocalDate.of(2026, 9, day).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
    )

    @Test fun historyFiltersAndGroupsByDay() {
        val all = listOf(
            movement(4, MovementType.WASTE, "Milk", 26, note = "spoiled"),
            movement(3, MovementType.USAGE, "Onions", 26),
            movement(2, MovementType.OPENING, "Onions", 25),
            movement(1, MovementType.ADJUSTMENT, "Milk", 24),
        )
        assertEquals(listOf(2L), filterMovements(all, HistoryFilter.STOCK_IN, "").map { it.id })
        assertEquals(listOf(4L), filterMovements(all, HistoryFilter.ALL, "SPOIL").map { it.id })
        assertEquals(listOf(4L, 1L), filterMovements(all, HistoryFilter.ALL, "milk").map { it.id })

        val days = groupByDay(all, ZoneOffset.UTC)
        assertEquals(listOf(26, 25, 24), days.map { it.date.dayOfMonth })
        assertEquals(listOf(4L, 3L), days.first().entries.map { it.id })
    }
}

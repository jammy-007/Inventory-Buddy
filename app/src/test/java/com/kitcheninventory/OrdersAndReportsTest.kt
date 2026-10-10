package com.kitcheninventory

import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.MovementType
import com.kitcheninventory.data.db.ReportMovement
import com.kitcheninventory.ui.items.ItemForm
import com.kitcheninventory.ui.items.filterItems
import com.kitcheninventory.ui.items.toEntity
import com.kitcheninventory.ui.items.validate
import com.kitcheninventory.ui.orders.groupOrders
import com.kitcheninventory.ui.orders.orderLines
import com.kitcheninventory.ui.orders.orderMessage
import com.kitcheninventory.ui.orders.orderQuantityError
import com.kitcheninventory.ui.orders.receiveOrder
import com.kitcheninventory.ui.orders.suggestedOrder
import com.kitcheninventory.ui.reports.ReportPeriod
import com.kitcheninventory.ui.reports.ReportRange
import com.kitcheninventory.ui.reports.buildReport
import com.kitcheninventory.ui.reports.stockValueByCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersAndReportsTest {
    private fun item(
        id: Long,
        onHand: Double,
        par: Double = 0.0,
        orderUpTo: Double = 0.0,
        supplierId: Long? = null,
        supplier: String? = null,
        category: String? = null,
        cost: Double = 2.0,
        barcode: String? = null,
    ) = ItemWithStock(id, "Item $id", 1, "kg", null, category, supplierId, supplier, cost, par, onHand, barcode, orderUpTo)

    @Test fun suggestsRefillToOrderUpToLevel() {
        assertEquals(17.0, suggestedOrder(item(1, onHand = 3.0, par = 5.0, orderUpTo = 20.0)), 1e-9)
        // No order-up-to level: refill to twice the reorder level.
        assertEquals(7.0, suggestedOrder(item(2, onHand = 3.0, par = 5.0)), 1e-9)
        // Not low, or no reorder level: nothing to order.
        assertEquals(0.0, suggestedOrder(item(3, onHand = 9.0, par = 5.0)), 0.0)
        assertEquals(0.0, suggestedOrder(item(4, onHand = 0.0)), 0.0)
        // Negative stock (usage recorded before the delivery) still refills to the target.
        assertEquals(12.0, suggestedOrder(item(5, onHand = -2.0, par = 5.0)), 1e-9)
    }

    @Test fun groupsLowItemsBySupplierWithNoSupplierLast() {
        val items = listOf(
            item(1, onHand = 1.0, par = 2.0),
            item(2, onHand = 1.0, par = 2.0, supplierId = 20, supplier = "zest foods"),
            item(3, onHand = 1.0, par = 2.0, supplierId = 10, supplier = "Acme"),
            item(4, onHand = 9.0, par = 2.0, supplierId = 10, supplier = "Acme"),
        )
        val low = groupOrders(items, includeAll = false)
        assertEquals(listOf("Acme", "zest foods", null), low.map { it.supplierName })
        assertEquals(listOf(3L), low.first().items.map { it.id })
        assertEquals(listOf(3L, 4L), groupOrders(items, includeAll = true).first().items.map { it.id })
    }

    @Test fun orderLinesSkipBlankZeroAndInvalid() {
        val group = groupOrders(listOf(item(1, 0.0, 1.0), item(2, 0.0, 1.0), item(3, 0.0, 1.0), item(4, 0.0, 1.0)), false).single()
        val lines = orderLines(group, mapOf(1L to "2,5", 2L to "0", 3L to "x"))
        assertEquals(listOf(1L to 2.5), lines.map { (i, q) -> i.id to q })
        assertNotNull(orderQuantityError("x"))
        assertNotNull(orderQuantityError("-1"))
        assertNull(orderQuantityError(""))
    }

    @Test fun orderMessageListsItemsAndReceivingAddsStock() {
        val lines = listOf(item(1, 0.0) to 12.0)
        val message = orderMessage("Acme", lines)
        assertTrue(message.startsWith("Hello Acme,"))
        assertTrue("- Item 1: 12 kg" in message)

        val movements = receiveOrder(lines, "Order from Acme", 7)
        assertEquals(12.0, movements.single().quantity, 0.0)
        assertEquals(MovementType.STOCK_IN, movements.single().type)
        assertEquals(2.0, movements.single().unitCost, 0.0)
    }

    @Test fun periodsCoverWholeDays() {
        val today = LocalDate.of(2026, 3, 15)
        assertEquals(LocalDate.of(2026, 3, 9) to LocalDate.of(2026, 3, 16), ReportPeriod.WEEK.range(today))
        assertEquals(LocalDate.of(2026, 3, 1) to LocalDate.of(2026, 3, 16), ReportPeriod.THIS_MONTH.range(today))
        assertEquals(LocalDate.of(2026, 2, 1) to LocalDate.of(2026, 3, 1), ReportPeriod.LAST_MONTH.range(today))
    }

    @Test fun reportRangesIncludeBothEnds() {
        val today = LocalDate.of(2026, 10, 10)
        val week = ReportRange.of(ReportPeriod.WEEK, today)
        assertEquals(LocalDate.of(2026, 10, 4), week.start)
        assertEquals(today, week.end)
        assertEquals(7L, week.days)
        assertEquals(30L, ReportRange.of(ReportPeriod.MONTH, today).days)
        assertEquals(30L, ReportRange.of(ReportPeriod.LAST_MONTH, today).days) // September

        assertEquals(1L, ReportRange(today, today).days)
        assertEquals(31L, ReportRange(LocalDate.of(2026, 9, 10), today).days) // 21 in Sep + 10 in Oct
        assertThrows(IllegalArgumentException::class.java) { ReportRange(today, today.minusDays(1)) }
    }

    private fun movement(itemId: Long, type: MovementType, quantity: Double, cost: Double, category: String? = null) =
        ReportMovement(itemId, "Item $itemId", category, "kg", type, quantity, cost)

    @Test fun reportTotalsUseRecordedCosts() {
        val report = buildReport(
            listOf(
                movement(1, MovementType.OPENING, 10.0, 1.0),
                movement(1, MovementType.STOCK_IN, 5.0, 2.0),
                movement(1, MovementType.USAGE, -3.0, 2.0, "Produce"),
                movement(2, MovementType.USAGE, -1.0, 10.0, "Dairy"),
                movement(2, MovementType.WASTE, -0.5, 10.0, "Dairy"),
                movement(1, MovementType.ADJUSTMENT, -1.0, 2.0),
            ),
        )
        assertEquals(10.0, report.received, 1e-9)
        assertEquals(16.0, report.used, 1e-9)
        assertEquals(5.0, report.wasted, 1e-9)
        assertEquals(-2.0, report.countCorrections, 1e-9)
        assertEquals(5.0 / 21.0 * 100, report.wastePercent!!, 1e-9)
        assertEquals(listOf("Item 2", "Item 1"), report.topUsed.map { it.label })
        assertEquals("3 kg", report.topUsed[1].detail)
        assertEquals(listOf("Dairy" to 10.0, "Produce" to 6.0), report.usedByCategory.map { it.label to it.value })
    }

    @Test fun emptyReportHasNoWastePercent() {
        assertNull(buildReport(emptyList()).wastePercent)
    }

    @Test fun stockValueByCategoryIgnoresNegativeStock() {
        val lines = stockValueByCategory(
            listOf(item(1, 5.0, category = "Dairy"), item(2, -3.0, category = "Dairy"), item(3, 1.0)),
        )
        assertEquals(listOf("Dairy" to 10.0, "No category" to 2.0), lines.map { it.label to it.value })
    }

    @Test fun barcodeIsSearchableAndValidated() {
        val items = listOf(item(1, 0.0, barcode = "4006381333931"), item(2, 0.0))
        assertEquals(listOf(1L), filterItems(items, "4006381333931", null, false).map { it.id })

        val form = ItemForm(name = "Pens", unitId = 1, barcode = " 123 ", orderUpTo = "10", parLevel = "4")
        assertTrue(validate(form, isNew = true, nameTaken = false).isEmpty)
        assertNotNull(validate(form, isNew = true, nameTaken = false, barcodeTaken = true).barcode)
        assertNotNull(validate(form.copy(orderUpTo = "3"), isNew = true, nameTaken = false).orderUpTo)
        val entity = form.toEntity(null)
        assertEquals("123", entity.barcode)
        assertEquals(10.0, entity.orderUpTo, 0.0)
        assertNull(form.copy(barcode = "  ").toEntity(null).barcode)
    }
}

package com.kitcheninventory.ui.orders

import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.MovementType
import com.kitcheninventory.data.db.StockMovementEntity
import com.kitcheninventory.ui.common.formatQuantity
import com.kitcheninventory.ui.common.parseNumber

/**
 * How much to order to bring [item] back to its order-up-to level, or to twice its reorder level
 * when none is set. Zero for items that are not low.
 */
fun suggestedOrder(item: ItemWithStock): Double {
    if (!item.isLowStock) return 0.0
    val target = if (item.orderUpTo > 0) item.orderUpTo else item.parLevel * 2
    return (target - item.onHand).coerceAtLeast(0.0)
}

/** The items one supplier can be asked for. [supplierId] is null for items without a supplier. */
data class OrderGroup(val supplierId: Long?, val supplierName: String?, val items: List<ItemWithStock>)

/**
 * Groups items into one order per supplier: suppliers by name, then items with no supplier.
 * Without [includeAll], only low-stock items are listed and suppliers with none are left out.
 */
fun groupOrders(items: List<ItemWithStock>, includeAll: Boolean): List<OrderGroup> =
    items
        .filter { includeAll || it.isLowStock }
        .groupBy { it.supplierId }
        .map { (supplierId, list) -> OrderGroup(supplierId, list.first().supplierName, list) }
        .sortedWith(compareBy<OrderGroup> { it.supplierId == null }.thenBy { it.supplierName?.lowercase() })

/** Order quantity check. Blank means "don't order" and is not an error. */
fun orderQuantityError(text: String): String? {
    if (text.isBlank()) return null
    val value = parseNumber(text) ?: return "Enter a number"
    return if (value < 0) "Can't be negative" else null
}

/** Items in [group] with a valid amount above zero in [quantities] (item id to typed text). */
fun orderLines(group: OrderGroup, quantities: Map<Long, String>): List<Pair<ItemWithStock, Double>> =
    group.items.mapNotNull { item ->
        val text = quantities[item.id].orEmpty()
        if (orderQuantityError(text) != null) return@mapNotNull null
        val amount = parseNumber(text)?.takeIf { it > 0 } ?: return@mapNotNull null
        item to amount
    }

/** A plain-text order ready to send by email, SMS or a chat app. */
fun orderMessage(supplierName: String?, lines: List<Pair<ItemWithStock, Double>>): String = buildString {
    appendLine(if (supplierName != null) "Hello $supplierName," else "Hello,")
    appendLine()
    appendLine("Please send the following:")
    lines.forEach { (item, amount) -> appendLine("- ${item.name}: ${formatQuantity(amount)} ${item.unitAbbreviation}") }
    appendLine()
    append("Thank you.")
}

/** Stock-in movements for a delivery that matches the order. */
fun receiveOrder(lines: List<Pair<ItemWithStock, Double>>, note: String, timestamp: Long): List<StockMovementEntity> =
    lines.map { (item, amount) ->
        StockMovementEntity(
            itemId = item.id,
            type = MovementType.STOCK_IN,
            quantity = amount,
            unitCost = item.unitCost,
            note = note,
            timestamp = timestamp,
        )
    }

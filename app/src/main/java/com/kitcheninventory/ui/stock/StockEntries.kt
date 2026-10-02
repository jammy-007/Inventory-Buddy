package com.kitcheninventory.ui.stock

import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.MovementType
import com.kitcheninventory.data.db.StockMovementEntity
import com.kitcheninventory.ui.common.parseNumber
import kotlin.math.abs

/** The kinds of stock change recorded from the Record screen. */
enum class EntryKind(val type: MovementType, val label: String, val sign: Int) {
    STOCK_IN(MovementType.STOCK_IN, "Stock in", 1),
    USAGE(MovementType.USAGE, "Usage", -1),
    WASTE(MovementType.WASTE, "Waste", -1),
}

/** Differences smaller than this are treated as zero, to ignore floating point noise. */
private const val EPSILON = 1e-9

/** Checks one typed quantity. Blank means "no entry" and is not an error. */
fun entryError(text: String): String? {
    if (text.isBlank()) return null
    val value = parseNumber(text) ?: return "Enter a number"
    return if (value <= 0) "Must be above 0" else null
}

/** Checks one typed count. Zero is a valid count; blank means "not counted". */
fun countError(text: String): String? {
    if (text.isBlank()) return null
    val value = parseNumber(text) ?: return "Enter a number"
    return if (value < 0) "Can't be negative" else null
}

/**
 * Turns typed quantities (item id to text) into signed stock movements. Blank and invalid entries
 * are skipped, so callers should check [entryError] first. Items that no longer exist are skipped.
 */
fun buildEntries(
    kind: EntryKind,
    quantities: Map<Long, String>,
    items: List<ItemWithStock>,
    note: String,
    timestamp: Long,
): List<StockMovementEntity> {
    val byId = items.associateBy { it.id }
    return quantities.mapNotNull { (itemId, text) ->
        val item = byId[itemId] ?: return@mapNotNull null
        if (text.isBlank() || entryError(text) != null) return@mapNotNull null
        val amount = parseNumber(text) ?: return@mapNotNull null
        StockMovementEntity(
            itemId = itemId,
            type = kind.type,
            quantity = kind.sign * amount,
            unitCost = item.unitCost,
            note = note.trim(),
            timestamp = timestamp,
        )
    }
}

/**
 * Turns a physical count into ADJUSTMENT movements: counted minus what the app expects. Items whose
 * count matches, or that were left blank, get no movement.
 */
fun buildCountAdjustments(
    counts: Map<Long, String>,
    items: List<ItemWithStock>,
    timestamp: Long,
): List<StockMovementEntity> {
    val byId = items.associateBy { it.id }
    return counts.mapNotNull { (itemId, text) ->
        val item = byId[itemId] ?: return@mapNotNull null
        if (text.isBlank() || countError(text) != null) return@mapNotNull null
        val counted = parseNumber(text) ?: return@mapNotNull null
        val difference = counted - item.onHand
        if (abs(difference) < EPSILON) return@mapNotNull null
        StockMovementEntity(
            itemId = itemId,
            type = MovementType.ADJUSTMENT,
            quantity = difference,
            unitCost = item.unitCost,
            note = "Count",
            timestamp = timestamp,
        )
    }
}

/** Items that were fine before [movements] and are at or below their reorder level after. */
fun newlyLowStock(items: List<ItemWithStock>, movements: List<StockMovementEntity>): List<ItemWithStock> {
    val change = movements.groupBy { it.itemId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
    return items.filter { item ->
        val delta = change[item.id] ?: return@filter false
        !item.isLowStock && item.copy(onHand = item.onHand + delta).isLowStock
    }
}

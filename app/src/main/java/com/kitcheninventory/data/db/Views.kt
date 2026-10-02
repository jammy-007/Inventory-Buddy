package com.kitcheninventory.data.db

/** An item joined with its lookups and current on-hand quantity, for list screens. */
data class ItemWithStock(
    val id: Long,
    val name: String,
    val unitId: Long,
    val unitAbbreviation: String,
    val categoryId: Long?,
    val categoryName: String?,
    val supplierId: Long?,
    val supplierName: String?,
    val unitCost: Double,
    val parLevel: Double,
    val onHand: Double,
    val barcode: String? = null,
    val orderUpTo: Double = 0.0,
) {
    val isLowStock: Boolean get() = isLowStock(onHand, parLevel)
    val stockValue: Double get() = onHand * unitCost
}

/**
 * Stock at or below the reorder level is low; a level of zero turns the alert off. The small
 * tolerance stops rounding leftovers from summed decimals (5.0000000001) hiding an alert.
 */
fun isLowStock(onHand: Double, parLevel: Double): Boolean = parLevel > 0 && onHand <= parLevel + 1e-9

/** A stock movement with the item's name and unit, for the history screen. */
data class MovementWithItem(
    val id: Long,
    val itemId: Long,
    val itemName: String,
    val unitAbbreviation: String,
    val type: MovementType,
    val quantity: Double,
    val unitCost: Double,
    val note: String,
    val timestamp: Long,
)

/** A stock movement with just what the reports screen needs. */
data class ReportMovement(
    val itemId: Long,
    val itemName: String,
    val categoryName: String?,
    val unitAbbreviation: String,
    val type: MovementType,
    val quantity: Double,
    val unitCost: Double,
)

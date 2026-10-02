package com.kitcheninventory.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A unit of measure such as kilogram (kg) or case. */
@Entity(tableName = "units", indices = [Index(value = ["name"], unique = true)])
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val abbreviation: String,
)

/** A grouping such as Produce or Dairy, used for filtering and later for reports. */
@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
)

/**
 * Something the restaurant stocks. The on-hand quantity is not stored here: it is the sum of
 * the item's [StockMovementEntity] rows, so every change to stock stays traceable.
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("unitId"), Index("categoryId"), Index("supplierId"), Index("name")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unitId: Long,
    val categoryId: Long? = null,
    val supplierId: Long? = null,
    /** Cost of one unit, in the restaurant's currency. */
    val unitCost: Double = 0.0,
    /** Reorder point: stock at or below this is low. Zero means no alert. */
    val parLevel: Double = 0.0,
    val notes: String = "",
    /** Archived items are hidden from the list but keep their history for reports. */
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class MovementType {
    /** Stock on hand when the item was first added. */
    OPENING,
    /** Delivery or purchase. */
    STOCK_IN,
    /** Used in the kitchen. */
    USAGE,
    /** Spoiled, dropped or thrown away. */
    WASTE,
    /** Correction from a physical count. */
    ADJUSTMENT,
}

/**
 * One change to an item's stock. [quantity] is signed: positive adds stock, negative removes it.
 * [unitCost] records the cost at the time so stock value reports stay correct when prices change.
 */
@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("itemId"), Index("timestamp"), Index("type")],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val type: MovementType,
    val quantity: Double,
    val unitCost: Double = 0.0,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

package com.kitcheninventory.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY name")
    fun observeAll(): Flow<List<UnitEntity>>

    @Insert
    suspend fun insertAll(units: List<UnitEntity>)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun get(id: Long): SupplierEntity?

    @Insert
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Delete
    suspend fun delete(supplier: SupplierEntity)
}

@Dao
abstract class ItemDao {
    @Query(
        """
        SELECT i.id, i.name, i.unitId, u.abbreviation AS unitAbbreviation,
               i.categoryId, c.name AS categoryName, i.supplierId, s.name AS supplierName,
               i.unitCost, i.parLevel,
               COALESCE((SELECT SUM(m.quantity) FROM stock_movements m WHERE m.itemId = i.id), 0) AS onHand
        FROM items i
        JOIN units u ON u.id = i.unitId
        LEFT JOIN categories c ON c.id = i.categoryId
        LEFT JOIN suppliers s ON s.id = i.supplierId
        WHERE i.archived = 0
        ORDER BY i.name COLLATE NOCASE
        """
    )
    abstract fun observeActiveWithStock(): Flow<List<ItemWithStock>>

    @Query("SELECT * FROM items WHERE id = :id")
    abstract suspend fun get(id: Long): ItemEntity?

    @Query("SELECT COUNT(*) FROM items WHERE archived = 0 AND name = :name COLLATE NOCASE AND id != :excludeId")
    abstract suspend fun countActiveWithName(name: String, excludeId: Long): Int

    @Insert
    abstract suspend fun insert(item: ItemEntity): Long

    @Update
    abstract suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET archived = 1, updatedAt = :now WHERE id = :id")
    abstract suspend fun archive(id: Long, now: Long)

    @Insert
    abstract suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE itemId = :itemId")
    abstract suspend fun onHand(itemId: Long): Double

    /** Creates an item and, when [openingStock] is above zero, its opening stock movement. */
    @Transaction
    open suspend fun insertWithOpeningStock(item: ItemEntity, openingStock: Double): Long {
        val id = insert(item)
        if (openingStock > 0) {
            insertMovement(
                StockMovementEntity(
                    itemId = id,
                    type = MovementType.OPENING,
                    quantity = openingStock,
                    unitCost = item.unitCost,
                    timestamp = item.createdAt,
                )
            )
        }
        return id
    }
}

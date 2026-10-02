package com.kitcheninventory.data.repo

import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.InventoryDatabase
import com.kitcheninventory.data.db.ItemEntity
import com.kitcheninventory.data.db.ItemWithStock
import com.kitcheninventory.data.db.MovementWithItem
import com.kitcheninventory.data.db.ReportMovement
import com.kitcheninventory.data.db.StockMovementEntity
import com.kitcheninventory.data.db.SupplierEntity
import com.kitcheninventory.data.db.UnitEntity
import kotlinx.coroutines.flow.Flow

/** Single entry point the screens use to read and write on-device data. */
class InventoryRepository(private val db: InventoryDatabase) {
    private val items = db.itemDao()
    private val suppliers = db.supplierDao()
    private val movements = db.movementDao()

    val units: Flow<List<UnitEntity>> = db.unitDao().observeAll()
    val categories: Flow<List<CategoryEntity>> = db.categoryDao().observeAll()
    val supplierList: Flow<List<SupplierEntity>> = suppliers.observeAll()
    val activeItems: Flow<List<ItemWithStock>> = items.observeActiveWithStock()

    suspend fun getItem(id: Long): ItemEntity? = items.get(id)
    suspend fun onHand(itemId: Long): Double = items.onHand(itemId)

    suspend fun isNameTaken(name: String, excludeId: Long = 0): Boolean =
        items.countActiveWithName(name.trim(), excludeId) > 0

    suspend fun isBarcodeTaken(barcode: String, excludeId: Long = 0): Boolean =
        items.countActiveWithBarcode(barcode.trim(), excludeId) > 0

    suspend fun addItem(item: ItemEntity, openingStock: Double): Long =
        items.insertWithOpeningStock(item, openingStock)

    suspend fun updateItem(item: ItemEntity) =
        items.update(item.copy(updatedAt = System.currentTimeMillis()))

    suspend fun archiveItem(id: Long) = items.archive(id, System.currentTimeMillis())

    suspend fun getSupplier(id: Long): SupplierEntity? = suppliers.get(id)
    suspend fun saveSupplier(supplier: SupplierEntity): Long =
        if (supplier.id == 0L) suppliers.insert(supplier) else supplier.id.also { suppliers.update(supplier) }
    suspend fun deleteSupplier(supplier: SupplierEntity) = suppliers.delete(supplier)

    /** Saves a batch of stock changes together and returns their ids for undo. */
    suspend fun recordMovements(list: List<StockMovementEntity>): List<Long> =
        if (list.isEmpty()) emptyList() else movements.insertAll(list)

    fun recentMovements(limit: Int = 500): Flow<List<MovementWithItem>> = movements.observeRecent(limit)

    fun movementsBetween(from: Long, to: Long): Flow<List<ReportMovement>> = movements.observeBetween(from, to)

    suspend fun deleteMovements(ids: List<Long>) {
        if (ids.isNotEmpty()) movements.delete(ids)
    }
}

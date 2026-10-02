package com.kitcheninventory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {
    /** Returns the new row ids, in order, so a batch can be undone. */
    @Insert
    suspend fun insertAll(movements: List<StockMovementEntity>): List<Long>

    @Query(
        """
        SELECT m.id, m.itemId, i.name AS itemName, u.abbreviation AS unitAbbreviation,
               m.type, m.quantity, m.unitCost, m.note, m.timestamp
        FROM stock_movements m
        JOIN items i ON i.id = m.itemId
        JOIN units u ON u.id = i.unitId
        ORDER BY m.timestamp DESC, m.id DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<MovementWithItem>>

    /** Movements from [from] (inclusive) to [to] (exclusive), with the item's category, for reports. */
    @Query(
        """
        SELECT m.itemId, i.name AS itemName, c.name AS categoryName, u.abbreviation AS unitAbbreviation,
               m.type, m.quantity, m.unitCost
        FROM stock_movements m
        JOIN items i ON i.id = m.itemId
        JOIN units u ON u.id = i.unitId
        LEFT JOIN categories c ON c.id = i.categoryId
        WHERE m.timestamp >= :from AND m.timestamp < :to
        """
    )
    fun observeBetween(from: Long, to: Long): Flow<List<ReportMovement>>

    @Query("DELETE FROM stock_movements WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)
}

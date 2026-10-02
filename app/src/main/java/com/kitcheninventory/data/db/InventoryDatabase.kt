package com.kitcheninventory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UnitEntity::class,
        CategoryEntity::class,
        SupplierEntity::class,
        ItemEntity::class,
        StockMovementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun unitDao(): UnitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun itemDao(): ItemDao
    abstract fun movementDao(): MovementDao

    companion object {
        const val FILE_NAME = "inventory.db"

        fun create(context: Context): InventoryDatabase =
            Room.databaseBuilder(context, InventoryDatabase::class.java, FILE_NAME)
                .addCallback(SeedCallback)
                .build()
    }
}

/** Fills in common restaurant units and categories the first time the database is created. */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultData.units.forEach { (name, abbreviation) ->
            db.execSQL("INSERT INTO units (name, abbreviation) VALUES (?, ?)", arrayOf(name, abbreviation))
        }
        DefaultData.categories.forEach { name ->
            db.execSQL("INSERT INTO categories (name) VALUES (?)", arrayOf(name))
        }
    }
}

object DefaultData {
    val units = listOf(
        "Kilogram" to "kg",
        "Gram" to "g",
        "Litre" to "L",
        "Millilitre" to "mL",
        "Piece" to "pc",
        "Dozen" to "dz",
        "Case" to "case",
        "Box" to "box",
        "Bag" to "bag",
        "Bottle" to "btl",
        "Can" to "can",
        "Pack" to "pack",
    )

    val categories = listOf(
        "Produce",
        "Meat & Poultry",
        "Seafood",
        "Dairy & Eggs",
        "Dry Goods",
        "Spices & Condiments",
        "Frozen",
        "Bakery",
        "Beverages",
        "Cleaning & Supplies",
    )
}

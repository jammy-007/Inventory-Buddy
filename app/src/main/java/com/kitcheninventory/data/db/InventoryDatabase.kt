package com.kitcheninventory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UnitEntity::class,
        CategoryEntity::class,
        SupplierEntity::class,
        ItemEntity::class,
        StockMovementEntity::class,
    ],
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/** Version 2 adds item barcodes and the order-up-to level used by purchase orders. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN barcode TEXT")
        db.execSQL("ALTER TABLE items ADD COLUMN orderUpTo REAL NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_barcode` ON `items` (`barcode`)")
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

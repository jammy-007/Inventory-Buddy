package com.kitcheninventory.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMovementType(type: MovementType): String = type.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = MovementType.valueOf(value)
}

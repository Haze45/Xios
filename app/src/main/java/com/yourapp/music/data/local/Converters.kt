package com.yourapp.music.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Lets Room store a List<String> column (e.g. a playlist's song IDs) as JSON text. */
class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = Json.encodeToString(list)

    @TypeConverter
    fun toStringList(data: String): List<String> =
        if (data.isBlank()) emptyList() else Json.decodeFromString(data)
}

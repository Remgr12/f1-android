package dev.remgr.f1.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun listToJson(list: List<String>): String = json.encodeToString(list)
    @TypeConverter fun jsonToList(value: String): List<String> = json.decodeFromString(value)
}

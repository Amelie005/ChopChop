package com.example.chopchoprecipeapp.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A converter class for Room.
 * @author Amelie Dzierzawa
 */
class Converters {
    /**
     * Converts a list of strings to a JSON string.
     * @param value the list of strings to convert
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String = Gson().toJson(value)

    /**
     * Converts a JSON string to a list of strings.
     * @param value the JSON string to convert
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
}
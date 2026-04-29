package com.sendra.data.local.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",") ?: emptyList()
    }
    
    @TypeConverter
    fun toStringList(list: List<String>): String {
        return list.joinToString(",")
    }
    
    @TypeConverter
    fun fromLongList(value: String?): List<Long> {
        return value?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }
    
    @TypeConverter
    fun toLongList(list: List<Long>): String {
        return list.joinToString(",")
    }
}

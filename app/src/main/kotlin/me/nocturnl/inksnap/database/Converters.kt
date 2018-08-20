package me.nocturnl.inksnap.database

import android.arch.persistence.room.TypeConverter
import org.joda.time.DateTime

class Converters {
    @TypeConverter fun toDateTime(value: Long?) = if (value == null) null else DateTime(value)
    
    @TypeConverter fun toLong(value: DateTime?) = value?.millis
}
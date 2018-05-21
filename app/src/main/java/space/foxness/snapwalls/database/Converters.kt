package space.foxness.snapwalls.database

import android.arch.persistence.room.TypeConverter
import java.util.*

class Converters {
    @TypeConverter
    fun toDate(value: Long?): Date? = if (value == null) null else Date(value)

    @TypeConverter
    fun toLong(value: Date?): Long? = value?.time

//    @TypeConverter
//    fun toString(id: UUID?): String? = id?.toString()
//
//    @TypeConverter
//    fun toUuid(s: String?): UUID? = if (s == null) null else UUID.fromString(s)
}

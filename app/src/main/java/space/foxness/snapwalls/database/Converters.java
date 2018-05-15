package space.foxness.snapwalls.database;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;
import java.util.UUID;

public class Converters
{
    @TypeConverter
    public static Date toDate(Long value)
    {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long toLong(Date value)
    {
        return value == null ? null : value.getTime();
    }

    @TypeConverter
    public static String toString(UUID id)
    {
        return id == null ? null : id.toString();
    }

    @TypeConverter
    public static UUID toUuid(String s)
    {
        return s == null ? null : UUID.fromString(s);
    }
}

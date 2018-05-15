package space.foxness.snapwalls.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import space.foxness.snapwalls.Post;

@Database(entities = { Post.class }, version = 1)
@TypeConverters({ Converters.class })
public abstract class AppDatabase extends RoomDatabase
{
    public abstract PostDao postDao();
}

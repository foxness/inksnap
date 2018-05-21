package space.foxness.snapwalls.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context

import space.foxness.snapwalls.Post
import space.foxness.snapwalls.SingletonHolder
import space.foxness.snapwalls.database.AppDatabase.Companion.databaseName

@Database(entities = [(Post::class)], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    
    companion object : SingletonHolder<AppDatabase, Context>({
        Room
                .databaseBuilder(it.applicationContext, AppDatabase::class.java, databaseName)
                .allowMainThreadQueries()
                .build()
        
        // allowMainThreadQueries() is a dirty hack
        // todo: fix this by going async
    }) {
        private const val databaseName = "queue"
    }
}

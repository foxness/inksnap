package space.foxness.snapwalls

import android.arch.persistence.room.Room
import android.content.Context
import space.foxness.snapwalls.database.AppDatabase
import space.foxness.snapwalls.database.PostDao

class Queue private constructor(context: Context) {

    private val dbDao: PostDao

    val posts: List<Post>
        get() = dbDao.posts

    init {
        dbDao = Room.databaseBuilder<AppDatabase>(context.applicationContext, AppDatabase::class.java, databaseName).allowMainThreadQueries().build().postDao()
        // allowMainThreadQueries() is a dirty hack
        // I shouldn't do database I/O on the main thread
        // but since Java doesn't have native async/await,
        // I'm not familiar with any Java async libraries,
        // and this app isn't in Kotlin
        // I have to use dirty hacks ._.
    }

    fun addPost(post: Post) = dbDao.addPost(post)

    fun getPost(id: Long): Post = dbDao.getPostById(id)
    
    fun updatePost(post: Post) = dbDao.updatePost(post)
    
    fun deletePost(id: Long) = dbDao.deletePostbyId(id)

    companion object {
        private const val databaseName = "queue"

        private var queueInstance: Queue? = null

        fun getInstance(context: Context): Queue {
            if (queueInstance == null)
                queueInstance = Queue(context)

            return queueInstance as Queue
        }
    }
}

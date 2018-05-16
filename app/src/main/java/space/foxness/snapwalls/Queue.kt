package space.foxness.snapwalls

import android.arch.persistence.room.Room
import android.content.Context
import java.util.UUID

import space.foxness.snapwalls.database.AppDatabase

class Queue private constructor(context: Context) {

    private val db: AppDatabase

    val posts: List<Post>
        get() = db.postDao().posts

    init {
        db = Room.databaseBuilder<AppDatabase>(context.applicationContext, AppDatabase::class.java, databaseName).allowMainThreadQueries().build()
        // allowMainThreadQueries() is a dirty hack
        // I shouldn't do database I/O on the main thread
        // but since Java doesn't have native async/await,
        // I'm not familiar with any Java async libraries,
        // and this app isn't in Kotlin
        // I have to use dirty hacks ._.
    }

    fun addPost(post: Post) = db.postDao().addPost(post)

    fun getPost(id: UUID): Post = db.postDao().getPostById(id)
    
    fun updatePost(post: Post) = db.postDao().updatePost(post)

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

package space.foxness.snapwalls

import android.content.Context
import space.foxness.snapwalls.database.AppDatabase
import space.foxness.snapwalls.database.PostDao

class Queue private constructor(context: Context)
{
    private val dbDao: PostDao = AppDatabase.getInstance(context).postDao()

    val posts: List<Post> get() = dbDao.posts

    fun addPost(post: Post) = dbDao.addPost(post)

    fun getPost(id: Int) = dbDao.getPostById(id)

    fun updatePost(post: Post) = dbDao.updatePost(post)

    fun deletePost(id: Int) = dbDao.deletePostbyId(id)

    companion object : SingletonHolder<Queue, Context>(::Queue)
    {
        fun List<Post>.onlyScheduled() = filter { it.scheduled }
        
        fun List<Post>.earliest() = minBy { it.intendedSubmitDate!!.millis }
    }
}

package space.foxness.snapwalls

import android.content.Context
import space.foxness.snapwalls.database.AppDatabase

class PostedPostRepository private constructor(context: Context)
{
    private val postedPostDao = AppDatabase.getInstance(context).postedPostDao()

    val postedPosts: List<PostedPost> get() = postedPostDao.postedPosts

    fun addPostedPost(postedPost: PostedPost) = postedPostDao.addPostedPost(postedPost)

    fun getPostedPost(id: String) = postedPostDao.getPostedPostById(id)

    fun updatePostedPost(postedPost: PostedPost) = postedPostDao.updatePostedPost(postedPost)

    fun deletePostedPost(id: String) = postedPostDao.deletePostedPostbyId(id)

    companion object : SingletonHolder<PostedPostRepository, Context>(::PostedPostRepository)
}

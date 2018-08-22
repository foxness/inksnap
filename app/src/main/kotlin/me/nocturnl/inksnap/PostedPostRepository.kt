package me.nocturnl.inksnap

import android.content.Context
import me.nocturnl.inksnap.database.AppDatabase

class PostedPostRepository private constructor(context_: Context)
{
    private val postedPostDao = AppDatabase.getInstance(context_.applicationContext).postedPostDao()

    val postedPosts: List<PostedPost> get() = postedPostDao.postedPosts

    fun addPostedPost(postedPost: PostedPost) = postedPostDao.addPostedPost(postedPost)

    fun getPostedPost(id: String) = postedPostDao.getPostedPostById(id)

    fun updatePostedPost(postedPost: PostedPost) = postedPostDao.updatePostedPost(postedPost)

    fun deletePostedPost(id: String) = postedPostDao.deletePostedPostbyId(id)

    companion object : SingletonHolder<PostedPostRepository, Context>(::PostedPostRepository)
}

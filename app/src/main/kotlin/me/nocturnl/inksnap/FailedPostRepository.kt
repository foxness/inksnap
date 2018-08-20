package me.nocturnl.inksnap

import android.content.Context
import me.nocturnl.inksnap.database.AppDatabase

class FailedPostRepository private constructor(context: Context)
{
    private val failedPostDao = AppDatabase.getInstance(context).failedPostDao()

    val failedPosts: List<FailedPost> get() = failedPostDao.failedPosts

    fun addFailedPost(failedPost: FailedPost) = failedPostDao.addFailedPost(failedPost)

    fun getFailedPost(id: String) = failedPostDao.getFailedPostById(id)

    fun updateFailedPost(failedPost: FailedPost) = failedPostDao.updateFailedPost(failedPost)

    fun deleteFailedPost(id: String) = failedPostDao.deleteFailedPostbyId(id)

    companion object : SingletonHolder<FailedPostRepository, Context>(::FailedPostRepository)
}

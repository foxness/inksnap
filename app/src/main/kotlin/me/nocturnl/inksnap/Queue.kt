package me.nocturnl.inksnap

import android.content.Context
import me.nocturnl.inksnap.database.AppDatabase

// todo: use cache instead of doing IO on every method call

class Queue private constructor(context_: Context) // todo: refactor the app to use viewmodel/livedata
{
    private val context: Context = context_.applicationContext
    
    private val postDao = AppDatabase.getInstance(context).postDao()
    
    private val thumbnailCache = ThumbnailCache.getInstance(context)

    val posts: List<Post> get() = postDao.posts

    fun addPost(post: Post) = postDao.addPost(post)

    fun getPost(id: String) = postDao.getPostById(id)

    fun updatePost(updatedPost: Post)
    {
        val postBeforeUpdate = getPost(updatedPost.id)!!
        
        val thumbnailNotNeededAnymore =
                postBeforeUpdate.isLink
                && (!updatedPost.isLink || postBeforeUpdate.content != updatedPost.content)

        if (thumbnailNotNeededAnymore && thumbnailCache.contains(postBeforeUpdate.getThumbnailId()))
        {
            thumbnailCache.remove(postBeforeUpdate.getThumbnailId())
        }
        
        postDao.updatePost(updatedPost)
    }

    fun deletePost(id: String)
    {
        val postBeforeDeletion = getPost(id)!!
        
        if (postBeforeDeletion.isLink && thumbnailCache.contains(postBeforeDeletion.getThumbnailId()))
        {
            thumbnailCache.remove(postBeforeDeletion.getThumbnailId())
        }
        
        postDao.deletePostbyId(id)
    }

    companion object : SingletonHolder<Queue, Context>(::Queue)
}

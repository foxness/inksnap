package me.nocturnl.inksnap.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update
import me.nocturnl.inksnap.PostedPost

@Dao
interface PostedPostDao {
    @get:Query("SELECT * FROM postedPosts")
    val postedPosts: List<PostedPost>

    @Query("SELECT * FROM postedPosts WHERE id = :id LIMIT 1")
    fun getPostedPostById(id: String): PostedPost?

    @Insert
    fun addPostedPost(postedPost: PostedPost): Long

    @Update
    fun updatePostedPost(postedPost: PostedPost)

//    @Delete
//    fun deletePost(post: Post)

    @Query("DELETE FROM postedPosts WHERE id = :id")
    fun deletePostedPostbyId(id: String)
}

package me.nocturnl.inksnap.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update
import me.nocturnl.inksnap.FailedPost

@Dao
interface FailedPostDao {
    @get:Query("SELECT * FROM failedPosts")
    val failedPosts: List<FailedPost>

    @Query("SELECT * FROM failedPosts WHERE id = :id LIMIT 1")
    fun getFailedPostById(id: String): FailedPost?

    @Insert
    fun addFailedPost(failedPost: FailedPost): Long

    @Update
    fun updateFailedPost(failedPost: FailedPost)

//    @Delete
//    fun deletePost(post: Post)

    @Query("DELETE FROM failedPosts WHERE id = :id")
    fun deleteFailedPostbyId(id: String)
}

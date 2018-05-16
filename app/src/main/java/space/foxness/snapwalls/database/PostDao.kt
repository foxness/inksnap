package space.foxness.snapwalls.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update
import java.util.UUID

import space.foxness.snapwalls.Post

@Dao
interface PostDao {
    @get:Query("SELECT * FROM queue")
    val posts: List<Post>

    @Query("SELECT * FROM queue WHERE id = :id LIMIT 1")
    fun getPostById(id: UUID): Post

    @Insert
    fun addPost(post: Post)

    @Update
    fun updatePost(post: Post)

    @Delete
    fun deletePost(post: Post)
}

package space.foxness.snapwalls.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;
import java.util.UUID;

import space.foxness.snapwalls.Post;

@Dao
public interface PostDao
{
    @Query("SELECT * FROM queue")
    List<Post> getPosts();
    
    @Query("SELECT * FROM queue WHERE id = :id LIMIT 1")
    Post getPostById(UUID id);
    
    @Insert
    void addPost(Post post);
    
    @Update
    void updatePost(Post post);
    
    @Delete
    void deletePost(Post post);
}

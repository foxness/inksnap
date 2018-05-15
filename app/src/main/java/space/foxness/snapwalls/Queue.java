package space.foxness.snapwalls;

import android.arch.persistence.room.Room;
import android.content.Context;

import java.util.List;
import java.util.UUID;

import space.foxness.snapwalls.database.AppDatabase;

public class Queue
{
    private static final String databaseName = "queue";
    
    private static Queue queueInstance;
    
    private AppDatabase db;
    
    public static Queue get(Context context)
    {
        if (queueInstance == null)
        {
            queueInstance = new Queue(context);
        }
        
        return queueInstance;
    }
    
    private Queue(Context context)
    {
        db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, databaseName).allowMainThreadQueries().build();
        // allowMainThreadQueries() is a dirty hack
        // I shouldn't do database I/O on the main thread
        // but since Java doesn't have native async/await,
        // I'm not familiar with any Java async libraries,
        // and this app isn't in Kotlin
        // I have to use dirty hacks ._.
    }
    
    public void addPost(Post post)
    {
        db.postDao().addPost(post);
    }
    
    public List<Post> getPosts()
    {
        return db.postDao().getPosts();
    }
    
    public Post getPost(UUID id)
    {
        return db.postDao().getPostById(id);
    }
    
    public void updatePost(Post post)
    {
        db.postDao().updatePost(post);
    }
}

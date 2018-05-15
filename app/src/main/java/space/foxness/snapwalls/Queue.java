package space.foxness.snapwalls;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Queue // todo: use Room to store the queue
{
    private static Queue queueInstance;
    
    private List<Post> posts;
    
    public static Queue get()
    {
        if (queueInstance == null)
            queueInstance = new Queue();
        
        return queueInstance;
    }
    
    private Queue()
    {
        posts = new ArrayList<>();
    }
    
    public void addPost(Post s)
    {
        posts.add(s);
    }
    
    public List<Post> getPosts()
    {
        return posts;
    }
    
    public Post getPost(UUID id)
    {
        for (Post s : posts)
            if (s.getId().equals(id))
                return s;
        
        return null;
    }
}

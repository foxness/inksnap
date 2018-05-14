package space.foxness.snapwalls;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Queue
{
    private static Queue queueInstance;
    
    private List<Submission> queue;
    
    public static Queue get()
    {
        if (queueInstance == null)
            queueInstance = new Queue();
        
        return queueInstance;
    }
    
    private Queue()
    {
        queue = new ArrayList<>();
        
        for (int i = 0; i < 100; ++i)
        {
            Submission s = new Submission();
            s.setTitle("title " + i);
            s.setSubreddit("subreddit" + i);
            s.setContent("content " + i);
            s.setType(i % 2 == 0);
            queue.add(s);
        }
    }
    
    public List<Submission> getQueue()
    {
        return queue;
    }
    
    public Submission getSubmission(UUID id)
    {
        for (Submission s : queue)
            if (s.getId().equals(id))
                return s;
        
        return null;
    }
}

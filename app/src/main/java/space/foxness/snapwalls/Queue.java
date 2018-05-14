package space.foxness.snapwalls;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Queue
{
    private static Queue queueInstance;
    
    private List<Submission> submissions;
    
    public static Queue get()
    {
        if (queueInstance == null)
            queueInstance = new Queue();
        
        return queueInstance;
    }
    
    private Queue()
    {
        submissions = new ArrayList<>();
        
        for (int i = 0; i < 100; ++i)
        {
            Submission s = new Submission();
            s.setTitle("title " + i);
            s.setSubreddit("test");
            s.setContent("content " + i);
            s.setType(i % 2 == 0);
            submissions.add(s);
        }
    }
    
    public List<Submission> getSubmissions()
    {
        return submissions;
    }
    
    public Submission getSubmission(UUID id)
    {
        for (Submission s : submissions)
            if (s.getId().equals(id))
                return s;
        
        return null;
    }
}

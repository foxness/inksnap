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
    }
    
    public void addSubmission(Submission s)
    {
        submissions.add(s);
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

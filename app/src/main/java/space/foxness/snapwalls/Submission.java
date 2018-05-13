package space.foxness.snapwalls;

import java.util.UUID;

public class Submission
{
    private UUID id;
    private String title;
    private String subreddit;
    private String content;
    private boolean isLink;
    
    public Submission()
    {
        id = UUID.randomUUID();
    }
    
    public Submission(Submission s)
    {
        this();
        title = s.title;
        subreddit = s.subreddit;
        content = s.content;
        isLink = s.isLink;
    }

    public UUID getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSubreddit()
    {
        return subreddit;
    }

    public void setSubreddit(String subreddit)
    {
        this.subreddit = subreddit;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public boolean isLink()
    {
        return isLink;
    }

    public void setLink(boolean link)
    {
        isLink = link;
    }
}

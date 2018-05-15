package space.foxness.snapwalls;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.UUID;

@Entity(tableName = "queue")
public class Post
{
    @PrimaryKey
    @NonNull
    private UUID id;

    @ColumnInfo(name = "title") // this isnt necessary, im just showing that you can customize it
    private String title;

    private String subreddit;

    private String content;

    private boolean type; // true - link, false - self/text
    
    public Post()
    {
        id = UUID.randomUUID();
    }
    
    public Post(Post s)
    {
        this();
        title = s.title;
        subreddit = s.subreddit;
        content = s.content;
        type = s.type;
    }

    public UUID getId()
    {
        return id;
    }
    
    public void setId(UUID id)
    {
        this.id = id;
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

    public boolean getType()
    {
        return type;
    }

    public void setType(boolean type)
    {
        this.type = type;
    }
}

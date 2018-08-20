package me.nocturnl.inksnap

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import org.joda.time.DateTime
import java.io.Serializable

@Entity(tableName = "postedPosts")
class PostedPost : Serializable
{
    @PrimaryKey
    var id = ""

    var title = ""

    var subreddit = ""

    var content = ""

    @ColumnInfo(name = "intended_submit_date")
    var intendedSubmitDate = DateTime(0)

    @ColumnInfo(name = "is_link")
    var isLink = false
    
    var url = ""

    companion object
    {
        fun from(post: Post, url: String): PostedPost
        {
            val postedPost = PostedPost()
            
            postedPost.id = post.id
            postedPost.title = post.title
            postedPost.subreddit = post.subreddit
            postedPost.content = post.content
            postedPost.intendedSubmitDate = post.intendedSubmitDate!!
            postedPost.isLink = post.isLink
            postedPost.url = url
            
            return postedPost
        }
    }
}
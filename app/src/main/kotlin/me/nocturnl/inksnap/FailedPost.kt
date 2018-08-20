package me.nocturnl.inksnap

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import org.joda.time.DateTime
import java.io.Serializable

@Entity(tableName = "failedPosts")
class FailedPost : Serializable
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
    
    @ColumnInfo(name = "fail_reason")
    var failReason = ""
    
    @ColumnInfo(name = "detailed_reason")
    var detailedReason = ""

    companion object
    {
        fun from(post: Post, failReason: String, detailedReason: String): FailedPost
        {
            val failedPost = FailedPost()

            failedPost.id = post.id
            failedPost.title = post.title
            failedPost.subreddit = post.subreddit
            failedPost.content = post.content
            failedPost.intendedSubmitDate = post.intendedSubmitDate!!
            failedPost.isLink = post.isLink
            failedPost.failReason = failReason
            failedPost.detailedReason = detailedReason

            return failedPost
        }
    }
}
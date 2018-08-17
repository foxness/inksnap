package space.foxness.snapwalls

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.webkit.URLUtil.isValidUrl
import org.joda.time.DateTime
import java.io.Serializable
import space.foxness.snapwalls.Util.sha256
import java.util.*

@Entity(tableName = "queue")
class Post : Serializable
{
    @PrimaryKey
    var id = ""

    var title = ""

    var subreddit = ""

    var content = ""

    @ColumnInfo(name = "intended_submit_date")
    var intendedSubmitDate: DateTime? = null

    var isLink = false

    var scheduled = false

    fun getThumbnailId() = content.sha256()
    
    fun isValid(dateMustBeValidForPostToBeValid: Boolean): Boolean
    {
        val notBlankTitle = title.isNotBlank()
        val validTitleLength = title.trim().length <= Reddit.POST_TITLE_LENGTH_LIMIT
        
        val validSubredditLength = subreddit.trim().length <= Reddit.SUBREDDIT_NAME_LENGTH_LIMIT
        val subredditCharRegex = """\w+""".toRegex()
        val validSubredditCharacters = subreddit.trim().matches(subredditCharRegex)
        
        val validContent = !isLink || isValidUrl(content)
        val validContentLength = isLink || content.length <= Reddit.POST_TEXT_LENGTH_LIMIT
        
        val isd = intendedSubmitDate
        // && has higher precedence than ||
        val validDate = !dateMustBeValidForPostToBeValid || isd != null && isd > DateTime.now()

        return notBlankTitle
               && validTitleLength
               && validSubredditLength
               && validSubredditCharacters
               && validContent
               && validContentLength
               && validDate
    }
    
    fun reasonWhyInvalid(dateMustBeValidForPostToBeValid: Boolean): String
    {
        return when
        {
            title.isBlank() -> "Title must not be blank"
            title.trim().length > Reddit.POST_TITLE_LENGTH_LIMIT -> "Title cannot have more than ${Reddit.POST_TITLE_LENGTH_LIMIT} characters"
            subreddit.trim().length > Reddit.SUBREDDIT_NAME_LENGTH_LIMIT -> "Subreddit name cannot have more than ${Reddit.SUBREDDIT_NAME_LENGTH_LIMIT} characters"
            !subreddit.trim().matches("""\w+""".toRegex()) -> "Subreddit name cannot contain non-alphanumeric characters"
            isLink && !isValidUrl(content) -> "Link posts must have a valid url"
            !isLink && content.length > Reddit.POST_TEXT_LENGTH_LIMIT -> "Post text cannot have more than ${Reddit.POST_TEXT_LENGTH_LIMIT} characters"
            dateMustBeValidForPostToBeValid ->
            {
                val isd = intendedSubmitDate
                when
                {
                    isd == null -> "Date should be set"
                    isd <= DateTime.now() -> "Date should not be in the past"
                    else -> throw Exception("how")
                }
            }
            else -> throw Exception("how")
        }
    }
    
    companion object
    {
        fun newInstance(): Post
        {
            val p = Post()
            p.id = UUID.randomUUID().toString()
            return p
        }
    }
}
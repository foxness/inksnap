package space.foxness.snapwalls

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.webkit.URLUtil.isValidUrl
import org.joda.time.DateTime
import java.io.Serializable
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
    
    fun isValid(dateMustBeValidForPostToBeValid: Boolean): Boolean
    {
        val notBlankTitle = title.isNotBlank()
        val notBlankSubreddit = subreddit.isNotBlank()
        val validContent = !isLink || isValidUrl(content)
        val validDate = !dateMustBeValidForPostToBeValid || intendedSubmitDate != null

        return notBlankTitle && notBlankSubreddit && validContent && validDate
    }
    
    fun reasonWhyInvalid(dateMustBeValidForPostToBeValid: Boolean): String?
    {
        return if (title.isBlank())
        {
            "Title must not be blank"
        }
        else if (subreddit.isBlank())
        {
            "Subreddit must not be blank"
        }
        else if (isLink && !isValidUrl(content))
        {
            "Link posts must have a valid url"
        }
        else if (dateMustBeValidForPostToBeValid && intendedSubmitDate == null)
        {
            "Date should be set"
        }
        else
        {
            null
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
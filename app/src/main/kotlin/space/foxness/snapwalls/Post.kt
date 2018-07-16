package space.foxness.snapwalls

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.webkit.URLUtil.isValidUrl
import org.joda.time.DateTime
import java.io.Serializable

@Entity(tableName = "queue")
class Post : Serializable
{
    @PrimaryKey(autoGenerate = true)
    var id = 0

    var title = ""

    var subreddit = ""

    var content = ""

    @ColumnInfo(name = "intended_submit_date")
    var intendedSubmitDate: DateTime? = null

    var isLink = false

    var scheduled = false
    
    val isValid: Boolean get()
    {
        val notBlankTitle = title.isNotBlank()
        val notBlankSubreddit = subreddit.isNotBlank()
        val validContent = !isLink || isValidUrl(content)

        return notBlankTitle && notBlankSubreddit && validContent
    }
    
    val reasonWhyInvalid: String? get()
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
        else
        {
            null
        }
    }
}
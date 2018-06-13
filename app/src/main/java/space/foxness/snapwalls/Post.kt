package space.foxness.snapwalls

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
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

    var type = false // true - link, false - self/text

    var scheduled = false
}
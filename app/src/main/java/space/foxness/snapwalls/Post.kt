package space.foxness.snapwalls

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import org.joda.time.DateTime
import java.io.Serializable

@Entity(tableName = "queue")
class Post : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    
    var title: String = ""

    var subreddit: String = ""

    var content: String = ""
    
    @ColumnInfo(name = "intended_submit_date")
    var intendedSubmitDate: DateTime? = null

    var type: Boolean = false // true - link, false - self/text
}

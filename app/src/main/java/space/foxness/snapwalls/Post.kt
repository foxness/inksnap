package space.foxness.snapwalls

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

import java.util.UUID

@Entity(tableName = "queue")
class Post {
    @PrimaryKey
    var id: UUID = UUID.randomUUID()

    @ColumnInfo(name = "title") // this isnt necessary, im just showing that you can customize it
    var title: String = ""

    var subreddit: String = ""

    var content: String = ""

    var type: Boolean = false // true - link, false - self/text
}

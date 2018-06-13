package space.foxness.snapwalls

import android.support.v4.app.Fragment
import android.util.Log
import android.webkit.URLUtil.isValidUrl
import android.widget.Toast
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder
import java.util.*

object Util
{
    const val APPNAME = "Snapwalls"

    const val STATE_LENGTH = 10

    private val periodformatter =
            PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2) // gives the '01'
                    .appendHours().appendSeparator(":").appendMinutes().appendSeparator(":")
                    .appendSecondsWithMillis().toFormatter()

    fun Fragment.toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    fun Any.log(text: String)
    {
        var className = this::class.java.simpleName
        if (className.isEmpty()) className = "Anonymous"

        Log.d("$APPNAME.$className", text)
    }

    fun Duration.toNice(): String = periodformatter.print(toPeriod()).dropLast(2)

    fun String.isImageUrl() = isValidUrl(this) && listOf(".png", ".jpg").any { this.endsWith(it) }

    fun randomState() = randomAlphaString(STATE_LENGTH)

    fun randomAlphaString(length: Int): String
    {
        val rand = Random()
        fun randomInt(min: Int, max: Int) = rand.nextInt(max - min + 1) + min // inclusive end

        val alpha = 'a'..'z' // possible characters of the string
        return List(length,
                    {
                        randomInt(alpha.first.toInt(),
                                  alpha.endInclusive.toInt()).toChar()
                    }).joinToString("")
    }
}
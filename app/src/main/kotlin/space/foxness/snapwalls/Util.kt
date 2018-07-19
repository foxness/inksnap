package space.foxness.snapwalls

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.URLUtil.isValidUrl
import android.widget.Toast
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object Util
{
    const val APPNAME = "Snapwalls"
    
    const val USER_AGENT = "Snapwalls by /u/foxneZz"

    const val STATE_LENGTH = 10

    private val periodformatter =
            PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2) // gives the '01'
                    .appendHours().appendSeparator(":").appendMinutes().appendSeparator(":")
                    .appendSecondsWithMillis().toFormatter()
    
    fun toast(ctx: Context?, text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()

    fun Fragment.toast(text: String) = toast(context, text)

    fun AppCompatActivity.toast(text: String) = toast(this, text)

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
        return List(length) {
            randomInt(alpha.first.toInt(),
                      alpha.endInclusive.toInt()).toChar()
        }.joinToString("")
    }
    
    fun timeLeftUntil(datetime: DateTime) = Duration(DateTime.now(), datetime)
    
    fun downloadBytesFromUrl(url: String): ByteArray?
    {
//        val headers = mapOf("User-Agent" to USER_AGENT)
//        val response = khttp.get(url = url, headers = headers)
//
//        if (response.statusCode != 200)
//        {
//            return null
//        }
//
//        return response.content
        
        val urlObj = URL(url)
        val connection = urlObj.openConnection() as HttpURLConnection
        
        try
        {
            val out = ByteArrayOutputStream()
            val inStream = connection.inputStream
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK)
            {
                return null
            }
            
            val buffer = ByteArray(1024)

            while (true)
            {
                val bytesRead = inStream.read(buffer)
                
                if (bytesRead == -1)
                {
                    break
                }
                
                out.write(buffer, 0, bytesRead)
            }
            
            out.close()
            return out.toByteArray()
        }
        finally
        {
            connection.disconnect()
        }
    }
    
    fun getBitmapFromBytes(bytes: ByteArray): Bitmap?
    {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
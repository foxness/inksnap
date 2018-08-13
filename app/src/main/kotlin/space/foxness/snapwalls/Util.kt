package space.foxness.snapwalls

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil.isValidUrl
import android.webkit.WebView
import android.widget.Toast
import khttp.responses.Response
import khttp.structures.authorization.Authorization
import kotlinx.coroutines.experimental.async
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

    const val MILLIS_IN_MINUTE: Long = 60 * 1000
    
    val titlePostComparator = Comparator { a: Post, b -> a.title.compareTo(b.title) }

    val datePostComparator = Comparator { a: Post, b ->
        val aNullDate = a.intendedSubmitDate == null
        val bNullDate = b.intendedSubmitDate == null
        
        if (aNullDate && bNullDate)
        {
            0
        }
        else if (aNullDate && !bNullDate)
        {
            1
        }
        else if (!aNullDate && bNullDate)
        {
            -1
        }
        else
        {
            a.intendedSubmitDate!!.compareTo(b.intendedSubmitDate!!)
        }
    }

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
    
    fun clearCookiesAndCache(webView: WebView)
    {
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
        webView.clearCache(true)
    }
    
    fun List<DateTime>.earliestFromNow(): DateTime?
    {
        val now = DateTime.now()
        return sorted().firstOrNull { it > now }
    }

    fun List<Post>.earliestPostDateFromNow(): DateTime?
    {
        return map { it.intendedSubmitDate!! }.earliestFromNow()
    }

    fun List<Post>.onlyFuture(): List<Post>
    {
        val now = DateTime.now()
        return filter { it.intendedSubmitDate!! > now }
    }

    fun List<Post>.onlyPast(): List<Post>
    {
        val now = DateTime.now()
        return filter { it.intendedSubmitDate!! <= now }
    }

    fun List<Post>.onlyScheduled() = filter { it.scheduled }

    fun List<Post>.earliest() = minBy { it.intendedSubmitDate!! }

    fun List<Post>.earliestPostDate(): DateTime?
    {
        return map { it.intendedSubmitDate!! }.min()
    }
    
    fun List<Post>.compatibleWithRatelimit(): Boolean
    {
        if (size < 2)
        {
            return true
        }
        
        val sorted = sortedWith(datePostComparator)
        val minWindow = Duration(Reddit.RATELIMIT_MS)
        
        for (i in 0..size - 2)
        {
            val current = sorted[i].intendedSubmitDate!!
            val next = sorted[i + 1].intendedSubmitDate!!
            val window = Duration(current, next)
            if (window <= minWindow)
            {
                return false
            }
        }
        
        return true
    }

    fun isNetworkAvailable(context: Context): Boolean
    {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ani = cm.activeNetworkInfo
        return ani?.isConnected == true // same as (ani?.isConnected ?: false)
    }
    
    fun httpGet(
            url: String,
            headers: Map<String, String> = mapOf(),
            data: Any? = null,
            auth: Authorization? = null): Response
    {
        return khttp.get(url = url, headers = headers, data = data, auth = auth)
    }

    fun httpPost(
            url: String,
            headers: Map<String, String> = mapOf(),
            data: Any? = null,
            auth: Authorization? = null): Response
    {
        return khttp.post(url = url, headers = headers, data = data, auth = auth)
    }
    
    fun httpGetAsync(
            url: String,
            headers: Map<String, String> = mapOf(),
            data: Any? = null,
            auth: Authorization? = null) = async {

        httpGet(url = url, headers = headers, data = data, auth = auth)
    }

    fun httpPostAsync(
            url: String,
            headers: Map<String, String> = mapOf(),
            data: Any? = null,
            auth: Authorization? = null) = async {

        httpPost(url = url, headers = headers, data = data, auth = auth)
    }
}
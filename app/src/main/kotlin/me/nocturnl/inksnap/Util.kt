package me.nocturnl.inksnap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil.isValidUrl
import android.webkit.WebView
import android.widget.Toast
//import khttp.responses.Response
//import khttp.structures.authorization.Authorization
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalTime
import org.joda.time.format.PeriodFormatterBuilder
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.*

object Util
{
    const val APPNAME = "Inksnap"
    
    const val USER_AGENT = "$APPNAME [${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME}] by /u/foxneZz"

    const val STATE_LENGTH = 10

    const val MILLIS_IN_MINUTE: Long = 60 * 1000
    
    private const val THUMBNAIL_SIZE = 200

    private val messageDigest = MessageDigest.getInstance("SHA-256")
    
    private val random = Random()

    private val randomStartTime = LocalTime(16, 0) // 16:00
    private val randomEndTime = LocalTime(18, 0) // 18:00
    
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
    
    fun String.sha256(): String
    {
        val bytes = messageDigest.digest(toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun showNoInternetMessage(ctx: Context)
    {
        // todo: extract, make toast long or use snackbar
        toast(ctx, "Oops, looks like you're not connected to the internet")
    }
    
    fun toast(ctx: Context?, text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()

    fun Fragment.toast(text: String) = toast(context, text)

    fun AppCompatActivity.toast(text: String) = toast(this, text)

    fun Any.log(text: String)
    {
        var className = this::class.java.simpleName
        if (className.isEmpty()) className = "Anonymous"

        Log.d("$APPNAME.$className", text)
    }

    private val periodformatter = PeriodFormatterBuilder()
            .appendDays()
            .appendSeparator(":")
            .minimumPrintedDigits(2)
            .printZeroAlways()
            .appendHours()
            .appendSeparator(":")
            .appendMinutes()
            .appendSeparator(":")
            .appendSeconds()
            .toFormatter()

    fun Duration.toNice(): String
    {
        return periodformatter.print(toPeriod())
    }

    fun String.isImageUrl() = isValidUrl(this) && listOf(".png", ".jpg").any { this.endsWith(it) }

    fun randomState() = randomAlphaString(STATE_LENGTH)

    fun randomAlphaString(length: Int): String
    {
        fun randomInt(min: Int, max: Int) = random.nextInt(max - min + 1) + min // inclusive end

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

    fun List<Post>.latestPostDate(): DateTime?
    {
        return map { it.intendedSubmitDate!! }.max()
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

    fun isNetworkAvailable(context: Context): Boolean
    {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ani = cm.activeNetworkInfo
        return ani?.isConnected == true // same as (ani?.isConnected ?: false)
    }
    
    fun convertToThumbnail(imageBytes: ByteArray): Bitmap
    {
        // todo: optimize
        val image = getBitmapFromBytes(imageBytes)!!
        val reduced = reduceBitmapToThumbnail(image)
        val squared = squareBitmap(reduced)
        return squared
    }

    private fun getBitmapFromBytes(bytes: ByteArray): Bitmap?
    {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    
    private fun reduceBitmapToThumbnail(srcBmp: Bitmap): Bitmap
    {
        val min = Math.min(srcBmp.width, srcBmp.height)
        val factor = THUMBNAIL_SIZE / min.toFloat()
        
        val newWidth = Math.round(srcBmp.width * factor)
        val newHeight = Math.round(srcBmp.height * factor)
        
        val resized = Bitmap.createScaledBitmap(srcBmp, newWidth, newHeight, true)
        return resized
    }
    
    private fun squareBitmap(srcBmp: Bitmap): Bitmap
    {
        return if (srcBmp.width >= srcBmp.height)
        {
            Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.width / 2 - srcBmp.height / 2,
                    0,
                    srcBmp.height,
                    srcBmp.height)
        }
        else
        {
            Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.height / 2 - srcBmp.width / 2,
                    srcBmp.width,
                    srcBmp.width)
        }
    }
    
    fun getVisibilityConstant(visible: Boolean) = if (visible) View.VISIBLE else View.INVISIBLE
    
    fun getVisibilityGoneConstant(visible: Boolean) = if (visible) View.VISIBLE else View.GONE
    
    fun generatePostDate(startingDate: DateTime, index: Int): DateTime
    {
        val randomMillis = randomStartTime.millisOfDay + random.nextInt(randomEndTime.millisOfDay - randomStartTime.millisOfDay)
        val time = Duration(randomMillis.toLong())
        
        return startingDate.plusDays(index).withTimeAtStartOfDay().plus(time)
    }
    
    fun generatePostDates(startingDate: DateTime, now: DateTime, length: Int): List<DateTime>
    {
        val dates = (0 until length).map { generatePostDate(startingDate, it) }.dropWhile { it < now }.toMutableList()
        while (dates.size < length)
            dates.add(generatePostDate(now, dates.size + 1))
        return dates
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
            auth: Authorization? = null)
            
            = GlobalScope.async {
        httpGet(url = url, headers = headers, data = data, auth = auth)
    }

    fun httpPostAsync(
            url: String,
            headers: Map<String, String> = mapOf(),
            data: Any? = null,
            auth: Authorization? = null)
            
            = GlobalScope.async {
        httpPost(url = url, headers = headers, data = data, auth = auth)
    }
}
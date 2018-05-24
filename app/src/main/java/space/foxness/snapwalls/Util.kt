package space.foxness.snapwalls

import android.support.v4.app.Fragment
import android.util.Log
import android.widget.Toast
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder

object Util {
    const val APPNAME = "Snapwalls"
    
    private val periodformatter = PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(2) // gives the '01'
            .appendHours()
            .appendSeparator(":")
            .appendMinutes()
            .appendSeparator(":")
            .appendSeconds()
            .toFormatter()
    
    fun Fragment.toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    
    fun Any.log(text: String) = Log.d("$APPNAME.${this::class.java.simpleName}", text)

    fun Duration.toNice(): String = periodformatter.print(toPeriod())
}
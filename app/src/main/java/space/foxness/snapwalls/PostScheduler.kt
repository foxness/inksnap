package space.foxness.snapwalls

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import org.joda.time.Duration

class PostScheduler(context: Context) {

    private val context = context.applicationContext
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun schedule(postId: Long, delay: Duration, wakeup: Boolean = true) {
        val datetime = (Duration(SystemClock.elapsedRealtime()) + delay)
        schedule(postId, datetime.millis, false, wakeup)
    }

    private fun schedule(postId: Long, millis: Long, rtc: Boolean, wakeup: Boolean = true) {
        
        val intent = SubmitService.newIntent(context, postId)
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, postId.toInt(), intent, 0)
        } else {
            PendingIntent.getService(context, postId.toInt(), intent, 0)
        }

        val type = if (rtc) {
            if (wakeup) 
                AlarmManager.RTC_WAKEUP 
            else 
                AlarmManager.RTC
        } else {
            if (wakeup) 
                AlarmManager.ELAPSED_REALTIME_WAKEUP 
            else 
                AlarmManager.ELAPSED_REALTIME
        }
        
        alarmManager.setExact(type, millis, pi)
    }
}
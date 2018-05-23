package space.foxness.snapwalls

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import org.joda.time.Duration
import org.joda.time.Instant

class PostScheduler(context: Context) {

    private val context = context.applicationContext!!
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun cancelScheduledPosts(postIds: List<Long>) {
        for (postId in postIds)
            cancelScheduledPost(postId)
    }
    
    fun scheduleDelayedPost(postId: Long, delay: Duration, wakeup: Boolean = true) {
        val datetime = (Duration(SystemClock.elapsedRealtime()) + delay)!!
        schedulePost(postId, datetime.millis, false, wakeup)
    }
    
    fun schedulePostAtSpecificTime(postId: Long, instant: Instant, wakeup: Boolean = true) {
        schedulePost(postId, instant.millis, true, wakeup)
    }
    
    fun isPostScheduled(postId: Long) = getPendingIntent(postId, PendingIntent.FLAG_NO_CREATE) != null
    
    fun cancelScheduledPost(postId: Long) {
        val pi = getPendingIntent(postId)!!
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun schedulePost(postId: Long, millis: Long, rtc: Boolean = false, wakeup: Boolean = true) {
        val type = getAlarmType(rtc, wakeup)
        val pi = getPendingIntent(postId)!!
        alarmManager.setExact(type, millis, pi)
    }
    
    private fun getPendingIntent(postId: Long, flags: Int = 0): PendingIntent? {
        val intent = SubmitService.newIntent(context, postId)
        val requestCode = postId.toInt()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }
    
    private fun getAlarmType(rtc: Boolean = false, wakeup: Boolean = true): Int {
        return if (rtc) {
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
    }
}
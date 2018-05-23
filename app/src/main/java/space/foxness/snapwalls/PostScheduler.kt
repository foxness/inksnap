package space.foxness.snapwalls

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.github.debop.kodatimes.times
import org.joda.time.DateTime
import org.joda.time.Duration

class PostScheduler(context: Context) { // todo: throw exception if schedule time is in the past

    private val context = context.applicationContext!!
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun schedulePeriodicPosts(postIds: List<Long>,
                              period: Duration,
                              initialDelay: Duration,
                              wakeup: Boolean = true) {

        val firstDate = Duration(SystemClock.elapsedRealtime()) + initialDelay
        val postDelays = HashMap(postIds
                .mapIndexed { i, postId -> postId to firstDate + period * i.toLong() }
                .toMap())
        
        scheduleDelayedPosts(postDelays, wakeup)
    }
    
    fun scheduleDelayedPosts(postDelays: HashMap<Long, Duration>, wakeup: Boolean = true) 
            = postDelays.forEach { postId, delay -> scheduleDelayedPost(postId, delay, wakeup) }
    
    // the post order is determined by the postIds order
    fun schedulePosts(postIds: List<Long>, schedule: Schedule, wakeup: Boolean = true) {
        
        val scheduleTimes = schedule.getScheduleTimes(DateTime.now(), postIds.size)
        if (postIds.size != scheduleTimes.size)
            throw RuntimeException("How did this even happen?") // this should never happen

        val postTimes = HashMap<Long, DateTime>()
        for ((index, time) in scheduleTimes.withIndex())
            postTimes[postIds[index]] = time
        
        schedulePosts(postTimes, wakeup)
    }

    fun schedulePosts(postTimes: HashMap<Long, DateTime>, wakeup: Boolean = true) 
            = postTimes.forEach { postId, dateTime -> schedulePostAtSpecificTime(postId, dateTime, wakeup) }
    
    fun cancelScheduledPosts(postIds: List<Long>)
            = postIds.forEach { cancelScheduledPost(it) }
    
    fun scheduleDelayedPost(postId: Long, delay: Duration, wakeup: Boolean = true) {
        val datetime = (Duration(SystemClock.elapsedRealtime()) + delay)
        schedulePost(postId, datetime.millis, false, wakeup)
    }
    
    fun schedulePostAtSpecificTime(postId: Long, datetime: DateTime, wakeup: Boolean = true)
            = schedulePost(postId, datetime.millis, true, wakeup)
    
    fun isPostScheduled(postId: Long)
            = getPendingIntent(postId, PendingIntent.FLAG_NO_CREATE) != null
    
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
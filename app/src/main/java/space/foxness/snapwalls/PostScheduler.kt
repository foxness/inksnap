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

    private val queue = Queue.getInstance(context)

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun isPostScheduled(post: Post) = post.scheduledDate != null
    
    fun cancelScheduledPosts(posts: List<Post>)
            = posts.forEach { cancelScheduledPost(it) }
    
    fun cancelScheduledPost(post: Post) {
        
        val esp = getEarliestScheduledPost()!!

        post.scheduledDate = null
        queue.updatePost(post)
        
        if (esp.id == post.id) {

            cancelScheduledService()
            
            val esp2 = getEarliestScheduledPost()
            if (esp2 != null)
                scheduleService(esp2.scheduledDate!!)
        }
    }
    
    fun schedulePeriodicPosts(posts: List<Post>,
                              period: Duration,
                              initialDelay: Duration) {

        val now = DateTime.now()
        posts.forEachIndexed { index, post ->
            val datetime = now + initialDelay + period * index.toLong()
            schedulePost(post, datetime)
        }
    }
    
    fun schedulePost(post: Post, datetime: DateTime) {
        
        val esp = getEarliestScheduledPost()

        post.scheduledDate = datetime
        queue.updatePost(post)
        
        val firstPost = esp == null
        val earlierPost = !firstPost && esp!!.scheduledDate!! > datetime
        
        if (firstPost || earlierPost) {
            if (earlierPost)
                cancelScheduledService()
            
            scheduleService(datetime)
        }
    }
    
    private fun scheduleService(datetime: DateTime) {
        val type = getAlarmType(false, true)
        val pi = getPendingIntent()!!
        
        val delay = Duration(DateTime.now(), datetime)
        val datetimeElapsed = Duration(SystemClock.elapsedRealtime()) + delay
        
        alarmManager.setExact(type, datetimeElapsed.millis, pi)
    }
    
    private fun cancelScheduledService() {
        val pi = getPendingIntent()!!
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun isServiceScheduled()
            = getPendingIntent(PendingIntent.FLAG_NO_CREATE) != null
    
    private fun getEarliestScheduledPost()
            = queue.posts.filter { it.scheduledDate != null }.minBy { it.scheduledDate!!.millis }

    private fun getPendingIntent(flags: Int = 0): PendingIntent? {
        val intent = SubmitService.newIntent(context) // TODO: OVERHAUL SUBMITSERVICE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, REQUEST_CODE, intent, flags)
        } else {
            PendingIntent.getService(context, REQUEST_CODE, intent, flags)
        }
    }

    companion object {
        private const val REQUEST_CODE = 0

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
}

//package space.foxness.snapwalls
//
//import android.app.AlarmManager
//import android.app.PendingIntent
//import android.content.Context
//import android.os.Build
//import android.os.SystemClock
//import com.github.debop.kodatimes.times
//import org.joda.time.DateTime
//import org.joda.time.Duration
//
//class PostScheduler(context: Context) { // todo: throw exception if schedule time is in the past
//
//    private val context = context.applicationContext!!
//    
//    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//    
//    fun schedulePeriodicPosts(postIds: List<Long>,
//                              period: Duration,
//                              initialDelay: Duration,
//                              wakeup: Boolean = true) {
//
//        val postDelays = HashMap(postIds
//                .mapIndexed { i, postId -> postId to initialDelay + period * i.toLong() }
//                .toMap())
//        
//        scheduleDelayedPosts(postDelays, wakeup)
//    }
//    
//    fun scheduleDelayedPosts(postDelays: HashMap<Long, Duration>, wakeup: Boolean = true) 
//            = postDelays.forEach { postId, delay -> scheduleDelayedPost(postId, delay, wakeup) }
//    
//    // the post order is determined by the postIds order
//    fun scheduleTimelyPosts(postIds: List<Long>, schedule: WeekSchedule, wakeup: Boolean = true) {
//        
//        val scheduleTimes = schedule.getScheduleTimes(DateTime.now(), postIds.size)
//        if (postIds.size != scheduleTimes.size)
//            throw RuntimeException("How did this even happen?") // this should never happen
//
//        val postTimes = HashMap<Long, DateTime>()
//        for ((index, time) in scheduleTimes.withIndex())
//            postTimes[postIds[index]] = time
//
//        scheduleTimelyPosts(postTimes, wakeup)
//    }
//
//    fun scheduleTimelyPosts(postTimes: HashMap<Long, DateTime>, wakeup: Boolean = true) 
//            = postTimes.forEach { postId, dateTime -> scheduleTimelyPost(postId, dateTime, wakeup) }
//    
//    fun cancelScheduledPosts(postIds: List<Long>)
//            = postIds.forEach { cancelScheduledPost(it) }
//    
//    fun scheduleDelayedPost(postId: Long, delay: Duration, wakeup: Boolean = true) {
//        val datetime = (Duration(SystemClock.elapsedRealtime()) + delay)
//        schedulePost(postId, datetime.millis, false, wakeup)
//    }
//    
//    fun scheduleTimelyPost(postId: Long, datetime: DateTime, wakeup: Boolean = true)
//            = schedulePost(postId, datetime.millis, true, wakeup)
//    
//    fun isPostScheduled(postId: Long)
//            = getPendingIntent(postId, PendingIntent.FLAG_NO_CREATE) != null
//    
//    fun cancelScheduledPost(postId: Long) {
//        val pi = getPendingIntent(postId)!!
//        alarmManager.cancel(pi)
//        pi.cancel()
//    }
//
//    private fun schedulePost(postId: Long, millis: Long, rtc: Boolean = false, wakeup: Boolean = true) {
//        val type = getAlarmType(rtc, wakeup)
//        val pi = getPendingIntent(postId)!!
//        alarmManager.setExact(type, millis, pi)
//    }
//    
//    private fun getPendingIntent(postId: Long, flags: Int = 0): PendingIntent? {
//        val intent = SubmitService.newIntent(context, postId)
//        val requestCode = postId.toInt()
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            PendingIntent.getForegroundService(context, requestCode, intent, flags)
//        } else {
//            PendingIntent.getService(context, requestCode, intent, flags)
//        }
//    }
//    
//    private fun getAlarmType(rtc: Boolean = false, wakeup: Boolean = true): Int {
//        return if (rtc) {
//            if (wakeup)
//                AlarmManager.RTC_WAKEUP
//            else
//                AlarmManager.RTC
//        } else {
//            if (wakeup)
//                AlarmManager.ELAPSED_REALTIME_WAKEUP
//            else
//                AlarmManager.ELAPSED_REALTIME
//        }
//    }
//}
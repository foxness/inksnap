package me.nocturnl.inksnap

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import org.joda.time.DateTime
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.earliest
import me.nocturnl.inksnap.Util.earliestPostDate
import me.nocturnl.inksnap.Util.onlyScheduled
import me.nocturnl.inksnap.Util.timeLeftUntil

// todo: make reddit not a singleton and rename it to reddit account

class PostScheduler private constructor(context: Context)
{
    private val context = context.applicationContext!!

    private val queue = Queue.getInstance(context)

    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // this method expects the queue to be divided into 2 segments
    // the first segment is the scheduled segment at the beginning
    // the last segment is the segment that will be scheduled
    fun scheduleUnscheduledPostsPeriodic(period: Duration)
    {
        val posts = queue.posts

        if (posts.size < 2)
        {
            throw Exception("Need at least 2 posts")
        }

        if (posts.first().intendedSubmitDate == null)
        {
            throw Exception("Can't infer the periodic schedule")
        }

        var onlyUnscheduledNow = false
        for (i in 1 until posts.size)
        {
            if (posts[i].intendedSubmitDate == null)
            {
                posts[i].intendedSubmitDate = posts[i - 1].intendedSubmitDate!! + period
                
                schedulePost(posts[i])
                
                onlyUnscheduledNow = true
            }
            else if (onlyUnscheduledNow)
            {
                throw Exception("You can't switch from unscheduled to scheduled posts")
            }
        }
    }

    fun cancelScheduledPosts(posts: List<Post>) = posts.forEach { cancelScheduledPost(it) }

    fun cancelScheduledPost(post: Post)
    {
        if (!post.scheduled)
        {
            return
        }
        
        val esp = queue.posts.onlyScheduled().earliest()!!

        post.scheduled = false
        queue.updatePost(post)

        if (esp.id == post.id)
        {
            cancelScheduledService()

            val esp2 = queue.posts.onlyScheduled().earliest()
            if (esp2 != null)
            {
                scheduleService(esp2.intendedSubmitDate!!)
            }
        }
    }

    fun schedulePeriodicPosts(posts: List<Post>, period: Duration, initialDelay: Duration)
    {
        val now = DateTime.now()
        posts.forEachIndexed { index, post ->
            post.intendedSubmitDate = now + initialDelay + Duration(period.millis * index)
            schedulePost(post)
        }
    }
    
    fun scheduleManualPosts(posts: List<Post>) = posts.forEach { schedulePost(it) }

    fun schedulePost(post: Post)
    {
        val esp = queue.posts.onlyScheduled().earliest()
        
        post.scheduled = true
        queue.updatePost(post)
        
        val firstPost = esp == null
        val earlierPost = !firstPost && esp!!.intendedSubmitDate!! > post.intendedSubmitDate!!
        
        if (firstPost || earlierPost)
        {
            if (earlierPost)
            {
                cancelScheduledService()
            }

            scheduleService(post.intendedSubmitDate!!)
        }
    }

    fun scheduleServiceForNextPost()
    {
        val earliest = queue.posts.earliestPostDate()!!
        if (earliest <= DateTime.now())
        {
            runServiceNow()
        }
        else
        {
            scheduleService(earliest)
        }
    }

    private fun scheduleService(datetime: DateTime)
    {
        val type = getAlarmType(false, true)
        val pi = getPendingIntent()!!

        val delay = timeLeftUntil(datetime)
        
        if (delay < Duration.ZERO)
        {
            throw Exception("Can't schedule the service to a past date")
        }
        
        val datetimeElapsed = Duration(SystemClock.elapsedRealtime()) + delay

        alarmManager.setExact(type, datetimeElapsed.millis, pi)
    }
    
    fun runServiceNow()
    {
        val intent = AutosubmitService.newIntent(context)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(intent)
        }
        else
        {
            context.startService(intent)
        }
    }

    private fun cancelScheduledService()
    {
        val pi = getPendingIntent()!!
        alarmManager.cancel(pi)
        pi.cancel()
    }

    fun isServiceScheduled() = getPendingIntent(PendingIntent.FLAG_NO_CREATE) != null

    private fun getPendingIntent(flags: Int = 0): PendingIntent?
    {
        val intent = AutosubmitService.newIntent(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            PendingIntent.getForegroundService(context, REQUEST_CODE, intent, flags)
        }
        else
        {
            PendingIntent.getService(context, REQUEST_CODE, intent, flags)
        }
    }

    companion object : SingletonHolder<PostScheduler, Context>(::PostScheduler)
    {
        private const val REQUEST_CODE = 0

        private fun getAlarmType(rtc: Boolean = false, wakeup: Boolean = true): Int
        {
            return if (rtc)
            {
                if (wakeup)
                {
                    AlarmManager.RTC_WAKEUP
                }
                else
                {
                    AlarmManager.RTC
                }
            }
            else
            {
                if (wakeup)
                {
                    AlarmManager.ELAPSED_REALTIME_WAKEUP
                }
                else
                {
                    AlarmManager.ELAPSED_REALTIME
                }
            }
        }
    }
}
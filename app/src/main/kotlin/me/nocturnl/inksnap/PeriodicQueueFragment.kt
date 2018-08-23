package me.nocturnl.inksnap

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.SeekBar
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.earliestPostDate
import me.nocturnl.inksnap.Util.log
import me.nocturnl.inksnap.Util.timeLeftUntil
import me.nocturnl.inksnap.Util.toNice
import me.nocturnl.inksnap.Util.toast

class PeriodicQueueFragment : QueueFragment()
{
    override val fragmentLayoutId = R.layout.fragment_queue_periodic
    
    override val allowIntendedSubmitDateEditing = false
    
    private lateinit var seekBar: SeekBar

    override fun onAutosubmitServiceDoneReceived(context: Context, intent: Intent)
    {
        super.onAutosubmitServiceDoneReceived(context, intent)
        
        // todo: do something with this?
        val successfullyPosted = AutosubmitService.getSuccessfullyPostedFromIntent(intent)
        
        if (queue.posts.isEmpty())
        {
            settingsManager.autosubmitEnabled = false
            settingsManager.timeLeft = settingsManager.period
            updateToggleViews(false)

            unregisterAutosubmitServiceDoneReceiver()
        }
        else
        {
            val earliestPostDate = queue.posts.earliestPostDate()!!
            val unpausedTimeLeft = timeLeftUntil(earliestPostDate)
            startTimer(unpausedTimeLeft)
        }
        
        unrestrictTimerToggle()

        updatePostList()
    }

    override fun onInitUi(v: View)
    {
        super.onInitUi(v)
        
        seekBar = v.findViewById(R.id.queue_seekbar)
        seekBar.max = SEEKBAR_MAX_VALUE
        val changeListener = object : SeekBar.OnSeekBarChangeListener
        {
            private var timeLeft: Duration = Duration.ZERO

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean)
            {
                if (!fromUser)
                {
                    return
                }

                val percentage = 1 - progress.toDouble() / SEEKBAR_MAX_VALUE
                val millis = settingsManager.period.millis * percentage
                val rounded = Math.round(millis)
                timeLeft = Duration(rounded)
                updateTimerText(timeLeft)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }

            override fun onStopTrackingTouch(seekBar: SeekBar)
            {
                settingsManager.timeLeft = timeLeft
            }
        }

        seekBar.setOnSeekBarChangeListener(changeListener)
    }

    override fun toggleAutosubmit(on: Boolean)
    {
        if (on)
        {
            if (!reddit.isLoggedIn)
            {
                toast("You must be signed in to autosubmit")
                return
            }

            if (queue.posts.isEmpty())
            {
                toast("No posts to autosubmit")
                return
            }

            settingsManager.autosubmitEnabled = true

            val timeLeft = settingsManager.timeLeft!!

            startTimerAndRegisterReceiver(timeLeft)

            postScheduler.schedulePeriodicPosts(queue.posts, settingsManager.period, timeLeft)

            log("Scheduled ${queue.posts.size} post(s)")
        }
        else
        {
            settingsManager.autosubmitEnabled = false

            unregisterAutosubmitServiceDoneReceiver()

            timerObject.cancel()

            val earliestPostDate = queue.posts.earliestPostDate()!!
            settingsManager.timeLeft = timeLeftUntil(earliestPostDate)

            postScheduler.cancelScheduledPosts(queue.posts.reversed()) // ...its for optimization

            log("Canceled ${queue.posts.size} scheduled post(s)")
        }

        updateToggleViews(on)
    }

    override fun onTimerTick(millisUntilFinished: Long)
    {
        super.onTimerTick(millisUntilFinished)
        updateTimerViews(Duration(millisUntilFinished))
    }

    override fun onTimerFinish()
    {
        super.onTimerFinish()
        // todo: remove the submitted item from post list?
    }

    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(autosubmitEnabled: Boolean) // todo: refactor to not use arg
    {
        timerToggle.text = if (autosubmitEnabled) "Turn off" else "Turn on"
        seekBar.isEnabled = !autosubmitEnabled
        
        if (autosubmitEnabled)
        {
            val timeLeft = timeLeftUntil(queue.posts.earliestPostDate()!!)
            startToggleRestrictorJob(timeLeft)
        }
        else
        {
            stopToggleRestrictorJob()
        }

        updateTimerViews(settingsManager.timeLeft!!)
    }

    private fun updateTimerViews(timeLeft: Duration)
    {
        updateSeekbarProgress(timeLeft)
        updateTimerText(timeLeft)
    }

    private fun updateSeekbarProgress(timeLeft: Duration)
    {
        val millis = timeLeft.millis
        val percentage = 1 - millis.toFloat() / settingsManager.period.millis
        val rounded = Math.round(percentage * SEEKBAR_MAX_VALUE)
        seekBar.progress = rounded
    }

    private fun updateTimerText(timeLeft: Duration)
    {
        timerText.text = timeLeft.toNice()
    }

    override fun onStart()
    {
        super.onStart()
        
        if (settingsManager.timeLeft == null)
        {
            settingsManager.timeLeft = settingsManager.period
        }

        if (settingsManager.autosubmitEnabled)
        {
            if (queue.posts.isEmpty())
            {
                settingsManager.autosubmitEnabled = false
                settingsManager.timeLeft = settingsManager.period
            }
            else
            {
                val earliestPostDate = queue.posts.earliestPostDate()!!
                val unpausedTimeLeft = timeLeftUntil(earliestPostDate)
                startTimerAndRegisterReceiver(unpausedTimeLeft)
            }
        }
        else
        {
            if (settingsManager.timeLeft!! > settingsManager.period)
            {
                settingsManager.timeLeft = settingsManager.period
            }
        }

        updateToggleViews(settingsManager.autosubmitEnabled)
        updatePostList()
    }

    override fun onStop()
    {
        super.onStop()

        if (settingsManager.autosubmitEnabled)
        {
            timerObject.cancel()
        }
        
        toggleRestrictorJob?.cancel()

        unregisterAutosubmitServiceDoneReceiver() // maybe move this into the if?
    }

    override fun onNewPostAdded(newPost: Post)
    {
        super.onNewPostAdded(newPost)
        
        queue.addPost(newPost)

        if (settingsManager.autosubmitEnabled)
        {
            postScheduler.scheduleUnscheduledPostsPeriodic(settingsManager.period)
        }
    }

    override fun onPostDeleted(deletedPostId: String)
    {
        super.onPostDeleted(deletedPostId)
        
        val runOnEnabledAutosubmit = { postBeforeChange: Post ->
            
            val earliestPostDate = queue.posts.earliestPostDate()!!
            val timeLeft = timeLeftUntil(earliestPostDate)
            postScheduler.cancelScheduledPosts(queue.posts)
            queue.deletePost(deletedPostId)

            if (queue.posts.isEmpty())
            {
                settingsManager.autosubmitEnabled = false
                settingsManager.timeLeft = settingsManager.period
            }
            else
            {
                postScheduler.schedulePeriodicPosts(queue.posts, settingsManager.period, timeLeft)
            }

            // todo: why is settingsManager.timeLeft nullable? maybe make it non-nullable?
        }

        val runOnDisabledAutosubmit = {
            queue.deletePost(deletedPostId)
        }
        
        postChangeSafeguard(deletedPostId, runOnEnabledAutosubmit, runOnDisabledAutosubmit)
    }

    companion object
    {
        private const val SEEKBAR_MAX_VALUE = 1000

        fun newInstance() = PeriodicQueueFragment()
    }
}
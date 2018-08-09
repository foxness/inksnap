package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.joda.time.Duration
import space.foxness.snapwalls.Util.compatibleWithRatelimit
import space.foxness.snapwalls.Util.earliestPostDateFromNow
import space.foxness.snapwalls.Util.log
import space.foxness.snapwalls.Util.onlyFuture
import space.foxness.snapwalls.Util.onlyPast
import space.foxness.snapwalls.Util.timeLeftUntil
import space.foxness.snapwalls.Util.toNice
import space.foxness.snapwalls.Util.toast

class ManualQueueFragment : QueueFragment()
{
    override val fragmentLayoutId = R.layout.fragment_queue_manual

    override val allowIntendedSubmitDateEditing = true

    override fun onAutosubmitServiceDoneReceived(context: Context, intent: Intent) // assumes that autosubmit is on
    {
        super.onAutosubmitServiceDoneReceived(context, intent)

        // todo: do something with this?
        val successfullyPosted = AutosubmitService.getSuccessfullyPostedFromIntent(intent)

        startTimerForEarliestPostDateFromNow()

        unrestrictTimerToggle()

        updatePostList()
    }

    override fun toggleAutosubmit(on: Boolean)
    {
        if (on)
        {
            if (!reddit.isLoggedIn)
            {
                toast("You must be logged into Reddit for that")
                return
            }
            
            if (queue.posts.onlyPast().isNotEmpty())
            {
                toast("You have a post date in the past")
                return
            }
            
            if (!queue.posts.compatibleWithRatelimit())
            {
                toast("A post cannot be scheduled within 10 minutes of another (a rule imposed by Reddit)")
                return
            }

            settingsManager.autosubmitEnabled = true

            val futurePosts = queue.posts.onlyFuture()
            if (futurePosts.isNotEmpty())
            {
                registerAutosubmitServiceDoneReceiver()

                postScheduler.scheduleManualPosts(futurePosts)

                log("Scheduled ${futurePosts.size} post(s)")
            }
        }
        else
        {
            settingsManager.autosubmitEnabled = false

            val futurePosts = queue.posts.onlyFuture()
            if (futurePosts.isNotEmpty())
            {
                postScheduler.cancelScheduledPosts(futurePosts)

                unregisterAutosubmitServiceDoneReceiver()

                log("Canceled ${futurePosts.size} scheduled post(s)")
            }
        }

        updateToggleViews(on)
    }

    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(autosubmitEnabled: Boolean) // todo: refactor to not use arg?
    {
        timerToggle.text = if (autosubmitEnabled) "Turn off" else "Turn on"

        var timeLeft: Duration? = null
        val earliestFromNow = queue.posts.earliestPostDateFromNow()
        if (autosubmitEnabled && earliestFromNow != null)
        {
            timeLeft = timeLeftUntil(earliestFromNow)
            startToggleRestrictorJob(timeLeft)
        }
        else
        {
            stopToggleRestrictorJob()
        }

        updateTimerText(timeLeft)
    }

    private fun updateTimerText(timeLeft: Duration?)
    {
        timerText.text = timeLeft?.toNice() ?: "---"
    }

    override fun onStart()
    {
        super.onStart()

        if (settingsManager.timeLeft == null)
        {
            settingsManager.timeLeft = settingsManager.period
        }

        // todo: handle autosubmit type switches
        // switching from periodic to manual leads to posts with no dates
        // posts with no dates cause manual to crash
        
        val earliestFromNow = queue.posts.earliestPostDateFromNow()
        if (earliestFromNow != null)
        {
            val timeLeft = timeLeftUntil(earliestFromNow)
            startTimer(timeLeft)

            if (settingsManager.autosubmitEnabled)
            {
                registerAutosubmitServiceDoneReceiver()
            }
        }

        updateToggleViews(settingsManager.autosubmitEnabled)
        updatePostList()
    }
    
    private fun startToggleRestrictorJob(timeLeftUntilPost: Duration)
    {
        val timeLeftUntilCantToggleAutosubmit = timeLeftUntilPost - Duration(AUTOSUBMIT_TOGGLE_THRESHOLD_MS)
        if (timeLeftUntilCantToggleAutosubmit > Duration.ZERO)
        {
            toggleRestrictorJob = launch(UI) {
                delay(timeLeftUntilCantToggleAutosubmit.millis)

                if (isActive)
                {
                    restrictTimerToggle()
                }
            }
        }
        else
        {
            restrictTimerToggle()
        }
    }
    
    private fun stopToggleRestrictorJob()
    {
        toggleRestrictorJob?.cancel()
        toggleRestrictorJob = null
    }
    
    private fun restrictTimerToggle()
    {
        timerToggle.isEnabled = false
    }
    
    private fun unrestrictTimerToggle()
    {
        timerToggle.isEnabled = true
    }

    override fun onStop()
    {
        super.onStop()
        
        val timerIsTicking = queue.posts.onlyFuture().isNotEmpty()
        if (timerIsTicking)
        {
            timerObject.cancel()
        }
        
        toggleRestrictorJob?.cancel()

        unregisterAutosubmitServiceDoneReceiver() // maybe move this into the if?
    }
    
    private fun startTimerForEarliestPostDateFromNow()
    {
        val earliestFromNow = queue.posts.earliestPostDateFromNow()
        if (earliestFromNow == null)
        {
            updateTimerText(null)
        }
        else
        {
            val timeLeft = timeLeftUntil(earliestFromNow)
            startTimer(timeLeft)
        }
    }

    override fun onTimerFinish()
    {
        super.onTimerFinish()
        // todo: remove the submitted item from post list?
        
        if (!settingsManager.autosubmitEnabled)
        {
            startTimerForEarliestPostDateFromNow()
        }
    }

    override fun onTimerTick(millisUntilFinished: Long)
    {
        super.onTimerTick(millisUntilFinished)
        updateTimerText(Duration(millisUntilFinished))
    }

    override fun onNewPostAdded(newPost: Post)
    {
        super.onNewPostAdded(newPost)
        
        queue.addPost(newPost)

        if (settingsManager.autosubmitEnabled)
        {
            postScheduler.schedulePost(newPost)
        }
    }

    override fun onPostDeleted(deletedPostId: String)
    {
        super.onPostDeleted(deletedPostId)

        val runOnEnabledAutosubmit = { postBeforeChange: Post ->
            
            postScheduler.cancelScheduledPost(postBeforeChange)
            queue.deletePost(deletedPostId)
        }

        val runOnDisabledAutosubmit = {
            queue.deletePost(deletedPostId)
        }
        
        postChangeSafeguard(deletedPostId, runOnEnabledAutosubmit, runOnDisabledAutosubmit)
    }
    
    companion object
    {
        fun newInstance() = ManualQueueFragment()
    }
}
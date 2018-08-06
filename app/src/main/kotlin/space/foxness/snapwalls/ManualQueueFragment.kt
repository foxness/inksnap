package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
        
        // assume period and autosubmit type never change while autosubmit is enabled

        if (settingsManager.timeLeft == null)
        {
            settingsManager.timeLeft = settingsManager.period
        }

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

    override fun onStop()
    {
        super.onStop()
        
        val timerIsTicking = queue.posts.onlyFuture().isNotEmpty()
        if (timerIsTicking)
        {
            timerObject.cancel()
        }

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
        queue.addPost(newPost)

        if (settingsManager.autosubmitEnabled)
        {
            postScheduler.schedulePost(newPost)
        }
    }

    override fun onPostEdited(editedPost: Post)
    {
        val oldPost = queue.getPost(editedPost.id)!!
        val shouldBeRescheduled = settingsManager.autosubmitEnabled
                && !editedPost.intendedSubmitDate!!.isEqual(oldPost.intendedSubmitDate)

        if (shouldBeRescheduled)
        {
            postScheduler.cancelScheduledPost(oldPost)
        }

        queue.updatePost(editedPost)
        
        if (shouldBeRescheduled)
        {
            postScheduler.schedulePost(editedPost)
        }
    }

    override fun onPostDeleted(deletedPostId: String)
    {
        if (settingsManager.autosubmitEnabled)
        {
            val deletedPost = queue.getPost(deletedPostId)!!
            postScheduler.cancelScheduledPost(deletedPost)
        }

        queue.deletePost(deletedPostId)
    }
    
    companion object
    {
        fun newInstance() = ManualQueueFragment()
    }
}
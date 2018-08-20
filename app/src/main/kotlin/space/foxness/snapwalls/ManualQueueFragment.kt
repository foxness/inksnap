package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import org.joda.time.Duration
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
    
    private lateinit var timerLabel: TextView // todo: move to queuefragment
    private lateinit var emptyView: TextView // todo: move to queuefragment

    override val allowIntendedSubmitDateEditing = true

    override fun onAutosubmitServiceDoneReceived(context: Context, intent: Intent) // assumes that autosubmit is on
    {
        super.onAutosubmitServiceDoneReceived(context, intent)

        // todo: do something with this?
        val successfullyPosted = AutosubmitService.getSuccessfullyPostedFromIntent(intent)
        
        val toastString = if (successfullyPosted)
        {
            "Your post has successfully been posted"
        }
        else
        {
            "There was an error when submitting your post"
        }
        
        toast(toastString)

        startTimerForEarliestPostDateFromNow()

        unrestrictTimerToggle()

        updatePostList()
    }

    override fun onInitUi(v: View)
    {
        super.onInitUi(v)
        
        timerLabel = v.findViewById(R.id.queue_timer_label)
        emptyView = v.findViewById(R.id.queue_empty_view)
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
        
        val earliestFromNow = queue.posts.earliestPostDateFromNow()
        
        if (earliestFromNow == null)
        {
            stopToggleRestrictorJob()
            updateTimerVisibility(false)
        }
        else
        {
            val timeLeft = timeLeftUntil(earliestFromNow)

            updateTimerText(timeLeft)
            updateTimerVisibility(true)

            if (autosubmitEnabled)
            {
                startToggleRestrictorJob(timeLeft)
            }
        }
    }
    
    private fun updateTimerVisibility(visible: Boolean)
    {
        val flag = Util.getVisibilityConstant(visible)
        timerText.visibility = flag
        timerLabel.visibility = flag
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
            updateTimerVisibility(false)
        }
        else
        {
            val timeLeft = timeLeftUntil(earliestFromNow)
            startTimer(timeLeft)
            updateTimerVisibility(true)
        }
    }

    override fun onPostsChanged(count: Int)
    {
        super.onPostsChanged(count)
        
        val emptyViewVisible = count == 0
        val recyclerViewVisible = !emptyViewVisible
        
        emptyView.visibility = Util.getVisibilityGoneConstant(emptyViewVisible)
        recyclerView.visibility = Util.getVisibilityGoneConstant(recyclerViewVisible)
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
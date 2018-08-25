package me.nocturnl.inksnap

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.earliestPostDateFromNow
import me.nocturnl.inksnap.Util.log
import me.nocturnl.inksnap.Util.onlyFuture
import me.nocturnl.inksnap.Util.onlyPast
import me.nocturnl.inksnap.Util.timeLeftUntil
import me.nocturnl.inksnap.Util.toNice
import me.nocturnl.inksnap.Util.toast

class ManualQueueFragment : QueueFragment()
{
    override val fragmentLayoutId = R.layout.fragment_queue_manual
    
    private lateinit var timerLabel: TextView // todo: move to queuefragment
    private lateinit var emptyView: TextView // todo: move to queuefragment

    override val allowIntendedSubmitDateEditing = true

    override fun onSubmissionServiceDoneReceived(context: Context, intent: Intent) // assumes that submission is on
    {
        super.onSubmissionServiceDoneReceived(context, intent)

        // todo: do something with this?
        val successfullyPosted = SubmissionService.getSuccessfullyPostedFromIntent(intent)
        
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

    override fun toggleSubmission(on: Boolean)
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

            settingsManager.submissionEnabled = true

            val futurePosts = queue.posts.onlyFuture()
            if (futurePosts.isNotEmpty())
            {
                registerSubmissionServiceDoneReceiver()

                postScheduler.scheduleManualPosts(futurePosts)

                log("Scheduled ${futurePosts.size} post(s)")
            }
        }
        else
        {
            settingsManager.submissionEnabled = false

            val futurePosts = queue.posts.onlyFuture()
            if (futurePosts.isNotEmpty())
            {
                postScheduler.cancelScheduledPosts(futurePosts)

                unregisterSubmissionServiceDoneReceiver()

                log("Canceled ${futurePosts.size} scheduled post(s)")
            }
        }

        updateToggleViews(on)
    }

    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(submissionEnabled: Boolean) // todo: refactor to not use arg?
    {
        timerToggle.text = if (submissionEnabled) "Turn off" else "Turn on"
        
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

            if (submissionEnabled)
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

        // todo: handle scheduling type type switches
        // switching from periodic to manual leads to posts with no dates
        // posts with no dates cause manual to crash
        
        val earliestFromNow = queue.posts.earliestPostDateFromNow()
        if (earliestFromNow != null)
        {
            val timeLeft = timeLeftUntil(earliestFromNow)
            startTimer(timeLeft)

            if (settingsManager.submissionEnabled)
            {
                registerSubmissionServiceDoneReceiver()
            }
        }

        updateToggleViews(settingsManager.submissionEnabled)
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

        unregisterSubmissionServiceDoneReceiver() // maybe move this into the if?
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
        
        if (!settingsManager.submissionEnabled)
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

        if (settingsManager.submissionEnabled)
        {
            postScheduler.schedulePost(newPost)
        }
    }

    override fun onPostDeleted(deletedPostId: String)
    {
        super.onPostDeleted(deletedPostId)

        val runOnEnabledSubmission = { postBeforeChange: Post ->
            
            postScheduler.cancelScheduledPost(postBeforeChange)
            queue.deletePost(deletedPostId)
        }

        val runOnDisabledSubmission = {
            queue.deletePost(deletedPostId)
        }
        
        postChangeSafeguard(deletedPostId, runOnEnabledSubmission, runOnDisabledSubmission)
    }
    
    companion object
    {
        fun newInstance() = ManualQueueFragment()
    }
}
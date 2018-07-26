package space.foxness.snapwalls

import android.annotation.SuppressLint
import org.joda.time.Duration
import space.foxness.snapwalls.Queue.Companion.earliest
import space.foxness.snapwalls.Util.log
import space.foxness.snapwalls.Util.timeLeftUntil
import space.foxness.snapwalls.Util.toNice
import space.foxness.snapwalls.Util.toast

class ManualQueueFragment : QueueFragment()
{
    override val fragmentLayoutId = R.layout.fragment_queue_manual

    override val allowIntendedSubmitDateEditing = true

    override fun onSubmitReceived() // assumes that autosubmit is on
    {
        super.onSubmitReceived()
        
        if (!settingsManager.autosubmitEnabled)
        {
            throw Exception("How the hey did this even happen?")
            // hint: it must have happened when autosubmitEnabled was turned off right before
            // the submit service started
        }

        if (queue.posts.isEmpty())
        {
            updateTimerText(null)
        }
        else
        {
            val timeLeft = timeLeftUntil(queue.posts.earliest()!!.intendedSubmitDate!!)
            startTimer(timeLeft)
        }

        updatePostList()
    }

    override fun toggleAutosubmit(on: Boolean)
    {
        if (on == settingsManager.autosubmitEnabled) // this should never happen
        {
            throw Exception("Can't change autosubmit to state it's already in")
        }

        if (on)
        {
            if (!reddit.isLoggedIn)
            {
                toast("You must be signed in to autosubmit")
                return
            }

            settingsManager.autosubmitEnabled = true

            if (queue.posts.isNotEmpty())
            {
                val earliestPost = queue.posts.earliest()!!
                val timeLeft = timeLeftUntil(earliestPost.intendedSubmitDate!!)

                startTimerAndRegisterReceiver(timeLeft)

                postScheduler.scheduleManualPosts(queue.posts)

                log("Scheduled ${queue.posts.size} post(s)")
            }
        }
        else
        {
            settingsManager.autosubmitEnabled = false

            if (queue.posts.isNotEmpty())
            {
                unregisterSubmitReceiver()

                timerObject.cancel()

                postScheduler.cancelScheduledPosts(queue.posts.reversed()) // ...its for optimization

                log("Canceled ${queue.posts.size} scheduled post(s)")
            }
        }

        updateToggleViews(on)
    }

    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(autosubmitEnabled: Boolean) // todo: refactor to not use arg
    {
        timerToggle.text = if (autosubmitEnabled) "Turn off" else "Turn on"

        var timeLeft: Duration? = null

        val earliestPost = queue.posts.earliest()
        if (autosubmitEnabled && earliestPost != null)
        {
            timeLeft = timeLeftUntil(earliestPost.intendedSubmitDate!!)
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

        // TODO: schedule posts that were added a date if autosubmit is on and type is manual

        // assume period and autosubmit type never change while autosubmit is enabled
        // todo: actually prohibit changing these values while autosubmit is on

        if (settingsManager.timeLeft == null)
        {
            settingsManager.timeLeft = settingsManager.period
        }

        if (queue.posts.isNotEmpty())
        {
            val earliestPost = queue.posts.earliest()!!
            
            // todo: handle the case where the earliest date is earlier than now
            // todo: on timer finish start the next earliest post timer
            
            val timeLeft = timeLeftUntil(earliestPost.intendedSubmitDate!!)
            startTimerAndRegisterReceiver(timeLeft)
        }

        updateToggleViews(settingsManager.autosubmitEnabled)
        updatePostList()
    }

    override fun onStop()
    {
        super.onStop()

        if (queue.posts.isNotEmpty())
        {
            timerObject.cancel()
        }

        unregisterSubmitReceiver() // maybe move this into the if?
    }

    override fun onTimerFinish()
    {
        super.onTimerFinish()
        // todo: remove the submitted item from post list?
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
}
package space.foxness.snapwalls

import android.annotation.SuppressLint
import org.joda.time.DateTime
import org.joda.time.Duration
import space.foxness.snapwalls.Util.earliestFromNow
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

        val earliestFromNow = getEarliestPostDateFromNow()
        if (earliestFromNow == null)
        {
            updateTimerText(null)
        }
        else
        {
            val timeLeft = timeLeftUntil(earliestFromNow)
            startTimer(timeLeft)
        }

        updatePostList()
    }
    
    private fun getEarliestPostDateFromNow(): DateTime? // todo: move to util?
    {
        return queue.posts.map { it.intendedSubmitDate!! }.earliestFromNow()
    }
    
    private fun getFuturePosts(): List<Post> // todo: move to util?
    {
        val now = DateTime.now()
        return queue.posts.filter { it.intendedSubmitDate!! > now }
    }

    override fun toggleAutosubmit(on: Boolean) // todo: handle posts that are earlier than now (prohibit them in post fragment and handle here)
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

            val futurePosts = getFuturePosts()
            if (futurePosts.isNotEmpty())
            {
                val earliest = getEarliestPostDateFromNow()!!
                val timeLeft = timeLeftUntil(earliest)
                startTimerAndRegisterReceiver(timeLeft)

                postScheduler.scheduleManualPosts(futurePosts)

                log("Scheduled ${futurePosts.size} post(s)")
            }
        }
        else
        {
            settingsManager.autosubmitEnabled = false

            val futurePosts = getFuturePosts()
            if (futurePosts.isNotEmpty())
            {
                unregisterSubmitReceiver()

                timerObject.cancel()

                postScheduler.cancelScheduledPosts(futurePosts)

                log("Canceled ${futurePosts.size} scheduled post(s)")
            }
        }

        updateToggleViews(on)
    }

    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(autosubmitEnabled: Boolean) // todo: refactor to not use arg
    {
        timerToggle.text = if (autosubmitEnabled) "Turn off" else "Turn on"

        var timeLeft: Duration? = null
        val earliestFromNow = getEarliestPostDateFromNow()
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

        // TODO: schedule posts that were added a date if autosubmit is on and type is manual

        // assume period and autosubmit type never change while autosubmit is enabled
        // todo: actually prohibit changing these values while autosubmit is on

        if (settingsManager.timeLeft == null)
        {
            settingsManager.timeLeft = settingsManager.period
        }

        val earliestFromNow = getEarliestPostDateFromNow()
        if (earliestFromNow != null)
        {
            // todo: on timer finish start the next earliest post timer

            val timeLeft = timeLeftUntil(earliestFromNow)
            startTimer(timeLeft)

            if (settingsManager.autosubmitEnabled)
            {
                registerSubmitReceiver()
            }
        }

        updateToggleViews(settingsManager.autosubmitEnabled)
        updatePostList()
    }

    override fun onStop()
    {
        super.onStop()
        
        val timerIsTicking = getFuturePosts().isNotEmpty()
        if (timerIsTicking)
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
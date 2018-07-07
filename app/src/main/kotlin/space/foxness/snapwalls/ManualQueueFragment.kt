package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import org.joda.time.Duration
import space.foxness.snapwalls.Queue.Companion.earliest
import space.foxness.snapwalls.Queue.Companion.onlyScheduled
import space.foxness.snapwalls.Util.log
import space.foxness.snapwalls.Util.timeLeftUntil
import space.foxness.snapwalls.Util.toNice
import space.foxness.snapwalls.Util.toast

class ManualQueueFragment : QueueFragment()
{
    override val fragmentLayoutId = R.layout.fragment_queue_manual

    override val allowIntendedSubmitDateEditing = true

    override fun onSubmitReceived()
    {
        super.onSubmitReceived()
        
        val scheduledPosts = queue.posts.onlyScheduled()

        if (scheduledPosts.isEmpty())
        {
            updateTimerText(null)
        }
        else
        {
            val timeLeft = timeLeftUntil(scheduledPosts.earliest()!!.intendedSubmitDate!!)
            startTimer(timeLeft)
        }

        updatePostList()
    }

    override fun toggleAutosubmit(on: Boolean)
    {
        if (on == settingsManager.autosubmitEnabled) // this should never happen
        {
            throw RuntimeException("Can't change autosubmit to state it's already in")
        }

        if (on)
        {
            if (!reddit.isLoggedIn)
            {
                toast("You must be signed in to autosubmit")
                return
            }

            settingsManager.autosubmitEnabled = true

            val manualPosts = queue.posts.filter { it.intendedSubmitDate != null }

            if (manualPosts.isNotEmpty())
            {
                val earliestPost = manualPosts.earliest()!!
                val timeLeft = timeLeftUntil(earliestPost.intendedSubmitDate!!)

                startTimerAndRegisterReceiver(timeLeft)

                postScheduler.scheduleManualPosts(manualPosts)

                log("Scheduled ${manualPosts.size} post(s)")
            }
        }
        else
        {
            settingsManager.autosubmitEnabled = false

            val scheduledPosts = queue.posts.onlyScheduled()

            if (scheduledPosts.isNotEmpty())
            {
                unregisterSubmitReceiver()

                timerObject.cancel()

                postScheduler.cancelScheduledPosts(scheduledPosts.reversed()) // ...its for optimization

                log("Canceled ${scheduledPosts.size} scheduled post(s)")
            }
        }

        updateToggleViews(on)
    }

    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(autosubmitEnabled: Boolean) // todo: refactor to not use arg
    {
        timerToggle.text = if (autosubmitEnabled) "Turn off" else "Turn on"

        var timeLeft: Duration? = null

        val earliestPost = queue.posts.onlyScheduled().earliest()
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

        val scheduledPosts = queue.posts.onlyScheduled()
        if (scheduledPosts.isNotEmpty())
        {
            val timeLeft = timeLeftUntil(scheduledPosts.earliest()!!.intendedSubmitDate!!)
            startTimerAndRegisterReceiver(timeLeft)
        }

        updateToggleViews(settingsManager.autosubmitEnabled)
        updatePostList()
    }

    override fun onStop()
    {
        super.onStop()

        val scheduledPosts = queue.posts.onlyScheduled()
        if (scheduledPosts.isNotEmpty())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        // TODO: UPDATE THIS
        
        when (requestCode)
        {
            REQUEST_CODE_NEW_POST ->
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    val newPost = PostFragment.getPostFromResult(data!!)!!

                    queue.addPost(newPost)

                    if (settingsManager.autosubmitEnabled && newPost.intendedSubmitDate != null)
                    {
                        postScheduler.schedulePost(newPost)
                    }
                }
            }
            
            REQUEST_CODE_EDIT_POST ->
            {
                when (resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        val changedPost = PostFragment.getPostFromResult(data!!)!!
                        
                        
                        
                        if (settingsManager.autosubmitEnabled)
                        {
                            
                            

                            // todo: handle this (when autosubmit is on)
                        }
                    }
                    
                    PostFragment.RESULT_CODE_DELETED ->
                    {
                        val deletedPostId = PostFragment.getDeletedPostIdFromResult(data!!)
                        
                        
                    }
                }
            }
        }
    }
}
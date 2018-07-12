package space.foxness.snapwalls

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import space.foxness.snapwalls.Queue.Companion.earliest
import space.foxness.snapwalls.Queue.Companion.onlyScheduled
import space.foxness.snapwalls.Util.isImageUrl
import java.io.PrintWriter
import java.io.StringWriter

// todo: add all posts that failed to be submitted to a 'failed' list
// todo: add network safeguard to auth

class AutosubmitService : Service()
{
    private lateinit var mServiceLooper: Looper
    private lateinit var mServiceHandler: ServiceHandler

    private inner class ServiceHandler(looper: Looper) : Handler(looper)
    {
        override fun handleMessage(msg: Message)
        {
            val ctx = this@AutosubmitService
            
            try
            {
                Log.log(ctx, "Autosubmit service has awoken")
                
                val queue = Queue.getInstance(ctx)
                val scheduledPosts = queue.posts.onlyScheduled()

                if (scheduledPosts.isEmpty())
                {
                    throw Exception("001: No scheduled posts found")
                }

                val post = scheduledPosts.earliest()!!

                val reddit = Autoreddit.getInstance(ctx).reddit

                val signedIn = reddit.isLoggedIn
                val notRatelimited = !reddit.isRestrictedByRatelimit
                val networkAvailable = isNetworkAvailable()

                val readyToPost = signedIn && notRatelimited && networkAvailable

                val sm = SettingsManager.getInstance(ctx)

                val debugDontPost = sm.debugDontPost

                val imgurAccount = Autoimgur.getInstance(ctx).imgurAccount

                val type = post.type
                val loggedIntoImgur = imgurAccount.isLoggedIn

                if (type)
                {
                    val oldUrl = post.content
                    post.content = UrlProcessor.process(post.content)
                    val newUrl = post.content

                    if (oldUrl != newUrl)
                    {
                        Log.log(ctx, "Recognized url:\nOld: \"$oldUrl\"\nNew: \"$newUrl\"")
                    }
                }

                val isImageUrl = post.content.isImageUrl()

                if (type && isImageUrl && loggedIntoImgur)
                {
                    Log.log(ctx, "Uploading ${post.content} to imgur...")

                    val imgurImg = imgurAccount.uploadImage(post.content)

                    Log.log(ctx, "Success. New link: ${imgurImg.link}")
                    
                    post.content = imgurImg.link

                    if (sm.wallpaperMode)
                    {
                        val oldTitle = post.title
                        post.title += " [${imgurImg.width}×${imgurImg.height}]"

                        Log.log(ctx, "Changed post title from \"$oldTitle\" to \"${post.title}\" before posting")
                    }
                }
                else
                {
                    if (!type)
                    {
                        Log.log(ctx, "Not uploading to imgur because it's not a link")
                    }

                    if (!isImageUrl)
                    {
                        Log.log(ctx, "Not uploading to imgur because it's not an image url")
                    }

                    if (!loggedIntoImgur)
                    {
                        Log.log(ctx, "Not uploading to imgur because not logged into imgur")
                    }
                }

                if (readyToPost || debugDontPost)
                {
                    val link: String?
                    try
                    {
                        link = reddit.submit(post, debugDontPost, RESUBMIT, SEND_REPLIES)
                    }
                    catch (error: Exception)
                    {
                        // todo: handle this
                        throw error
                    }

                    Log.log(ctx, "Successfully submitted a post. Link: $link")

                    queue.deletePost(post.id) // todo: move to archive or something
                    Log.log(ctx, "Deleted the submitted post from the database")

                    val submittedAllPosts = queue.posts.isEmpty()
                    if (submittedAllPosts)
                    {
                        SettingsManager.getInstance(ctx).autosubmitEnabled = false
                        Log.log(ctx, "Ran out of posts and disabled autosubmit")
                    }
                    else
                    {
                        val ps = PostScheduler.getInstance(ctx)
                        ps.scheduleServiceForNextPost()
                        Log.log(ctx, "Scheduled service for the next post")
                    }

                    val broadcastIntent = Intent(POST_SUBMITTED)
                    broadcastIntent.putExtra(EXTRA_SUBMITTED_ALL_POSTS, submittedAllPosts)
                    LocalBroadcastManager.getInstance(ctx)
                            .sendBroadcast(broadcastIntent)
                }
                else
                {
                    Log.log(ctx, constructErrorMessage(post, signedIn, notRatelimited, networkAvailable))
                }
            }
            catch (exception: Exception)
            {
                val errors = StringWriter()
                exception.printStackTrace(PrintWriter(errors))
                val stacktrace = errors.toString()
                
                val errorMsg = "AN EXCEPTION HAS OCCURED. STACKTRACE:\n$stacktrace"
                Log.log(ctx, errorMsg)

                val builder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID) // todo: use different notification channel here
                val notification = builder
                        .setContentTitle("An exception has occurred")
                        .setContentText("An exception has occurred while submitting")
                        .setSmallIcon(R.drawable.snapwalls_icon)
                        .build()

                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(ERROR_NOTIFICATION_ID, notification)
            }
            finally
            {
                stopSelf(msg.arg1)

                // BIG NOTE: stopSelf(msg.arg1) MUST BE CALLED BEFORE RETURNING
                // DON'T RETURN WITHOUT CALLING IT
            }
        }
    }

    private fun isNetworkAvailable(): Boolean
    {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ani = cm.activeNetworkInfo
        return ani?.isConnected == true // same as (ani?.isConnected ?: false)
    }

    override fun onCreate()
    {
        val thread = HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()

        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel()
    {
        val nc = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                                     NOTIFICATION_CHANNEL_NAME,
                                     NotificationManager.IMPORTANCE_DEFAULT)

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(nc)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        // todo: remove the possibility of 2 parallel submissions

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            createNotificationChannel()
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = builder.setContentTitle(getString(R.string.app_name))
                .setContentText("Submitting...").setSmallIcon(R.drawable.snapwalls_icon).build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)

        val msg = mServiceHandler.obtainMessage()
        msg.arg1 = startId
        mServiceHandler.sendMessage(msg) // todo: how is this different from 'msg.sendToTarget()'?

        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object
    {
        const val EXTRA_SUBMITTED_ALL_POSTS = "submittedAllPosts"

        const val POST_SUBMITTED = "postSubmitted"

        private const val SEND_REPLIES = true
        private const val RESUBMIT = true

        private const val SERVICE_NOTIFICATION_ID = 1 // must not be 0
        private const val ERROR_NOTIFICATION_ID = 2
        private const val NOTIFICATION_CHANNEL_NAME = "Main"
        private const val NOTIFICATION_CHANNEL_ID = NOTIFICATION_CHANNEL_NAME

        fun newIntent(context: Context) = Intent(context, AutosubmitService::class.java)

        // assumes that not all of the arguments are true
        private fun constructErrorMessage(post: Post,
                                          signedIn: Boolean,
                                          notRatelimited: Boolean,
                                          networkAvailable: Boolean): String
        {
            val reasonsDidntPost = mutableListOf<String>()

            val notSignedInReason = "wasn't signed in"
            val ratelimitedReason = "was ratelimited"
            val networkNotAvailableReason = "network wasn't available"

            if (!signedIn) reasonsDidntPost.add(notSignedInReason)
            if (!notRatelimited) reasonsDidntPost.add(ratelimitedReason)
            if (!networkAvailable) reasonsDidntPost.add(networkNotAvailableReason)

            val postString = "the post titled '${post.title}' with ID ${post.id}"

            var errorMessage = "Couldn't submit $postString because "

            reasonsDidntPost.forEachIndexed { index, reason ->
                errorMessage += if (index == reasonsDidntPost.size - 1) reason
                else "$reason and "
            }

            return errorMessage
        }
    }
}
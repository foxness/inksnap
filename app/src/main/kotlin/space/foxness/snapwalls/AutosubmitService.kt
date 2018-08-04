package space.foxness.snapwalls

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import kotlinx.coroutines.experimental.runBlocking
import org.joda.time.DateTime
import org.joda.time.Duration
import space.foxness.snapwalls.Util.earliest
import space.foxness.snapwalls.Util.isImageUrl
import space.foxness.snapwalls.Util.onlyScheduled
import space.foxness.snapwalls.Util.toNice
import java.io.PrintWriter
import java.io.StringWriter

// todo: add all posts that failed to be submitted to a 'failed' list
// todo: add network safeguard to auth

class AutosubmitService : Service()
{
    private lateinit var serviceLooper: Looper
    private lateinit var serviceHandler: ServiceHandler
    
    private lateinit var notificationFactory: NotificationFactory

    private inner class ServiceHandler(looper: Looper) : Handler(looper)
    {
        suspend fun handle(msg: Message)
        {
            val ctx = this@AutosubmitService
            val log = Log.getInstance(ctx)
            val queue = Queue.getInstance(ctx)

            var post: Post? = null
            var successfullyPosted = false
            
            try
            {
                log.log("Autosubmit service has awoken")
                val scheduledPosts = queue.posts.onlyScheduled()

                if (scheduledPosts.isEmpty())
                {
                    throw Exception("No scheduled posts found") // this should never happen
                }

                post = scheduledPosts.earliest()!!

                val reddit = Autoreddit.getInstance(ctx).reddit

                val signedIn = reddit.isLoggedIn
                val notRatelimited = !reddit.isRestrictedByRatelimit
                val networkAvailable = isNetworkAvailable()
                
                if (signedIn && notRatelimited && networkAvailable)
                {
                    val imgurAccount = Autoimgur.getInstance(ctx).imgurAccount

                    val isLinkPost = post.isLink
                    val loggedIntoImgur = imgurAccount.isLoggedIn

                    if (isLinkPost)
                    {
                        val directUrl = ServiceProcessor.tryGetDirectUrl(post.content)

                        if (directUrl != null)
                        {
                            log.log("Recognized url:\nOld: \"${post.content}\"\nNew: \"$directUrl\"")
                            post.content = directUrl
                        }
                    }

                    val isImageUrl = post.content.isImageUrl()

                    if (isLinkPost && isImageUrl && loggedIntoImgur)
                    {
                        log.log("Uploading ${post.content} to imgur...")

                        val imgurImg = imgurAccount.uploadImage(post.content)

                        log.log("Success. New link: ${imgurImg.link}")

                        post.content = imgurImg.link

                        val sm = SettingsManager.getInstance(ctx)
                        if (sm.wallpaperMode)
                        {
                            val oldTitle = post.title
                            post.title += " [${imgurImg.width}×${imgurImg.height}]"

                            log.log("Changed post title from \"$oldTitle\" to \"${post.title}\" before posting")
                        }
                    }
                    else
                    {
                        if (!isLinkPost)
                        {
                            log.log("Not uploading to imgur because it's not a link")
                        }

                        if (!isImageUrl)
                        {
                            log.log("Not uploading to imgur because it's not an image url")
                        }

                        if (!loggedIntoImgur)
                        {
                            log.log("Not uploading to imgur because not logged into imgur")
                        }
                    }
                    
                    val link: String?
                    try
                    {
                        link = reddit.submit(post, false, RESUBMIT, SEND_REPLIES)
                    }
                    catch (error: Exception)
                    {
                        // todo: handle this
                        throw error
                    }
                    
                    successfullyPosted = true

                    val realSubmittedTime = DateTime.now()

                    notificationFactory.showSuccessNotification(post.title)

                    log.log("Successfully submitted a post. Link: $link")

                    val difference = Duration(post.intendedSubmitDate, realSubmittedTime)

                    log.log("Real vs intended time difference: ${difference.toNice()}")
                    
                    val postedPost = PostedPost.from(post, link)
                    val postedPostRepository = PostedPostRepository.getInstance(ctx)
                    postedPostRepository.addPostedPost(postedPost)

                    log.log("Added to posted post repository")
                    
                    queue.deletePost(post.id)
                    
                    log.log("Deleted the submitted post from the database")

                    val submittedAllPosts = queue.posts.isEmpty()
                    if (submittedAllPosts)
                    {
                        SettingsManager.getInstance(ctx).autosubmitEnabled = false
                        log.log("Ran out of posts and disabled autosubmit")
                    }
                    else
                    {
                        val ps = PostScheduler.getInstance(ctx)
                        ps.scheduleServiceForNextPost()
                        log.log("Scheduled service for the next post")
                    }

                    val broadcastIntent = Intent(POST_SUBMITTED)
                    broadcastIntent.putExtra(EXTRA_SUBMITTED_ALL_POSTS, submittedAllPosts) // todo: handle the intent extra
                    LocalBroadcastManager.getInstance(ctx).sendBroadcast(broadcastIntent)
                }
                else
                {
                    log.log(constructErrorMessage(post, signedIn, notRatelimited, networkAvailable))
                }
            }
            catch (exception: Exception)
            {
                val errors = StringWriter()
                exception.printStackTrace(PrintWriter(errors))
                val stacktrace = errors.toString()

                val errorMsg = "AN EXCEPTION HAS OCCURED. STACKTRACE:\n$stacktrace"
                log.log(errorMsg)
                
                val failReason = exception.message!!
                val detailedReason = stacktrace
                
                val failedPost = if (post == null)
                {
                    val p = Post.newInstance()
                    p.content = "SP: $successfullyPosted"
                    p.title = "placeholder title"
                    p.subreddit = "placeholder subreddit"
                    FailedPost.from(p, failReason, detailedReason)
                }
                else
                {
                    val fp = FailedPost.from(post, failReason, detailedReason)
                    queue.deletePost(post.id)
                    fp
                }
                
                val failedPostRepository = FailedPostRepository.getInstance(ctx)
                failedPostRepository.addFailedPost(failedPost)

                notificationFactory.showErrorNotification()
            }
            finally
            {
                stopSelf(msg.arg1)

                // BIG NOTE: stopSelf(msg.arg1) MUST BE CALLED BEFORE RETURNING
                // DON'T RETURN WITHOUT CALLING IT
            }
        }
        
        override fun handleMessage(msg: Message)
        {
            runBlocking { handle(msg) }
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

        serviceLooper = thread.looper
        serviceHandler = ServiceHandler(serviceLooper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        // todo: remove the possibility of 2 parallel submissions
        
        // todo: include the post title in the service notification
        notificationFactory = NotificationFactory.getInstance(this)
        val serviceNotification = notificationFactory.getServiceNotification()
        startForeground(NotificationFactory.SERVICE_NOTIFICATION_ID, serviceNotification)

        val msg = serviceHandler.obtainMessage()
        msg.arg1 = startId
        serviceHandler.sendMessage(msg) // todo: how is this different from 'msg.sendToTarget()'?

        return Service.START_NOT_STICKY // todo: use coroutines instead of ServiceHandler?
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object
    {
        const val EXTRA_SUBMITTED_ALL_POSTS = "submittedAllPosts"

        const val POST_SUBMITTED = "postSubmitted"

        private const val SEND_REPLIES = true
        private const val RESUBMIT = true

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
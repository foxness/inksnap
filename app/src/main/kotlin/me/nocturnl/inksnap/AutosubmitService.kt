package me.nocturnl.inksnap

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import kotlinx.coroutines.experimental.runBlocking
import org.joda.time.DateTime
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.isImageUrl
import me.nocturnl.inksnap.Util.onlyScheduled
import me.nocturnl.inksnap.Util.toNice
import java.io.PrintWriter
import java.io.StringWriter

class AutosubmitService : Service()
{
    private lateinit var serviceLooper: Looper
    private lateinit var serviceHandler: ServiceHandler
    
    private lateinit var notificationFactory: NotificationFactory

    private inner class ServiceHandler(looper: Looper) : Handler(looper)
    {
        fun generateFailedPost(failReason: String, detailedReason: String, post: Post?)
        {
            val underlyingPost = if (post == null)
            {
                val placeholder = Post.newInstance()
                placeholder.content = "placeholder content"
                placeholder.title = "placeholder title"
                placeholder.subreddit = "placeholder subreddit"
                placeholder
            }
            else
            {
                post
            }
            
            val failedPost = FailedPost.from(underlyingPost, failReason, detailedReason)
            
            val context = this@AutosubmitService
            
            val log = Log.getInstance(context)
            val failedPostRepository = FailedPostRepository.getInstance(context)
            failedPostRepository.addFailedPost(failedPost)
            
            if (post != null)
            {
                val queue = Queue.getInstance(context)
                queue.deletePost(post.id)
            }
            
            log.log(detailedReason)
            notificationFactory.showErrorNotification()
        }
        
        // todo: refactor this whole method
        suspend fun handle(msg: Message) // todo: refactor out of ServiceHandler to outer?
        {
            val ctx = this@AutosubmitService
            val log = Log.getInstance(ctx)
            val queue = Queue.getInstance(ctx)

            var post: Post? = null
            var successfullyPosted = false
            
            try
            {
                log.log("Autosubmit service has awoken")
                
                // technically the "onlyScheduled()" part is not necessary because all of them are scheduled
                val scheduledPosts = queue.posts.onlyScheduled().sortedBy { it.intendedSubmitDate!! }

                if (scheduledPosts.isEmpty())
                {
                    throw Exception("No scheduled posts found")
                }
                
                val now = DateTime.now()
                var lastFailedPostIndex: Int? = null
                
                for (i in scheduledPosts.indices)
                {
                    val realDelay = Duration(scheduledPosts[i].intendedSubmitDate!!, now)
                    if (realDelay > Duration(POST_DELAY_LIMIT_MS))
                    {
                        lastFailedPostIndex = i
                    }
                    else
                    {
                        break
                    }
                }
                
                log.log("Failed posts: ${(lastFailedPostIndex ?: -1) + 1}")
                
                if (lastFailedPostIndex != null)
                {
                    val failedPosts = scheduledPosts.take(lastFailedPostIndex + 1)
                    
                    val failReason = "No service" // todo: extract < & v
                    val detailedReason = "The service that submits scheduled posts could not be run at appropriate time either due to the android system not running it at appropriate time or due to the device being turned off"
                    for (failedPost in failedPosts)
                    {
                        generateFailedPost(failReason, detailedReason, failedPost)
                    }
                    
                    if (scheduledPosts.size > lastFailedPostIndex + 1)
                    {
                        val candidatePost = scheduledPosts[lastFailedPostIndex + 1]
                        
                        // not checking for period end because that's already been checked previously when calculating lastFailedPostIndex
                        if (candidatePost.intendedSubmitDate!! < now)
                        {
                            post = candidatePost
                        }
                    } // else all posts were failed and we have nothing to post
                }
                else
                {
                    post = scheduledPosts.first()
                }
                
                if (post != null)
                {
                    val reddit = Autoreddit.getInstance(ctx).reddit

                    val signedIn = reddit.isLoggedIn
                    if (!signedIn)
                    {
                        throw Exception("Wasn't logged into Reddit")
                    }

                    if (!Util.isNetworkAvailable(ctx))
                    {
                        val failReason = "No internet"
                        val detailedReason = "The device was not connected to the internet while the post was being submitted"

                        generateFailedPost(failReason, detailedReason, post)
                    }
                    else
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
                            link = reddit.submit(post, RESUBMIT, SEND_REPLIES)
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
                    }
                }
            }
            catch (exception: Exception)
            {
                val errors = StringWriter()
                exception.printStackTrace(PrintWriter(errors))
                val stacktrace = errors.toString()

                val errorMsg = "AN EXCEPTION HAS OCCURED. STACKTRACE:\n$stacktrace"
                log.log(errorMsg)
                
                val failReason = exception.message ?: exception.toString()
                val detailedReason = stacktrace
                
                generateFailedPost(failReason, detailedReason, post)
                
                // todo: broadcast intent that you havent submitted
            }
            finally
            {
                // technically onlyScheduled() is not necessary
                if (queue.posts.onlyScheduled().isEmpty())
                {
                    log.log("Ran out of posts")
                }
                else
                {
                    val ps = PostScheduler.getInstance(ctx)
                    ps.scheduleServiceForNextPost()
                    log.log("Scheduled service for the next post")
                }
                
                val broadcastIntent = Intent(AUTOSUBMIT_SERVICE_DONE)
                broadcastIntent.putExtra(EXTRA_SUCCESSFULLY_POSTED, successfullyPosted)

                val lbm = LocalBroadcastManager.getInstance(ctx)
                lbm.sendBroadcast(broadcastIntent)
                
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
        private const val EXTRA_SUCCESSFULLY_POSTED = "successfullyPosted"

        private const val AUTOSUBMIT_SERVICE_DONE = "autosubmitServiceDone"

        private const val SEND_REPLIES = true
        private const val RESUBMIT = true
        
        private val POST_DELAY_LIMIT_MS: Long = 10 * 60 * 1000 // 10 minutes

        fun newIntent(context: Context) = Intent(context, AutosubmitService::class.java)
        
        fun getSuccessfullyPostedFromIntent(broadcastIntent: Intent): Boolean
        {
            return broadcastIntent.getBooleanExtra(EXTRA_SUCCESSFULLY_POSTED, false)
        }
        
        fun getAutosubmitServiceDoneBroadcastIntentFilter(): IntentFilter
        {
            return IntentFilter(AUTOSUBMIT_SERVICE_DONE)
        }
    }
}
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
import space.foxness.snapwalls.Util.log

class SubmitService : Service() {
    
    private lateinit var mServiceLooper: Looper
    private lateinit var mServiceHandler: ServiceHandler
    
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {

            log("I am trying to submit...")

            // todo: add all posts that failed to be submitted to a 'failed' list
            // todo: add network safeguard to auth

            // BIG NOTE: stopSelf(msg.arg1) MUST BE CALLED BEFORE RETURNING
            // DON'T RETURN WITHOUT CALLING IT

            val intent = msg.obj as Intent
            val postId = intent.getLongExtra(EXTRA_POST_ID, -1)
            val post = Queue.getInstance(this@SubmitService).getPost(postId)
            val goodPost = post != null

            val reddit =  Autoreddit.getInstance(this@SubmitService).reddit
            val signedIn = reddit.isSignedIn
            val notRatelimited = !reddit.isRestrictedByRatelimit
            
            val networkAvailable = isNetworkAvailable()
            val notDebugging = !DEBUG_DONT_POST
            
            val everythingGood = goodPost 
                    && signedIn 
                    && notRatelimited 
                    && networkAvailable 
                    && notDebugging
            
            // todo: make reddit.submit() async !!
            if (everythingGood) {
                reddit.submit(post!!, { error, link ->
                    if (error != null) {
                        log("ERROR DURING SUBMISSION: ${error.message}")
                        throw error
                    }

                    log("Successfully submitted a post. Link: $link")
                })
            } else {
                log(constructErrorMessage(post, goodPost, signedIn, notRatelimited, networkAvailable, notDebugging))
            }

            stopSelf(msg.arg1)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ani = cm.activeNetworkInfo
        return ani?.isConnected == true // same as (ani?.isConnected ?: false)
    }

    override fun onCreate() {
        val thread = HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()

        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper)
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        
        val nc = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        nm.createNotificationChannel(nc)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // todo: remove the possibility of 2 parallel submissions
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()
        
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = builder
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Submitting...")
                .setSmallIcon(R.drawable.snapwalls_icon)
                .build()
        
        startForeground(NOTIFICATION_ID, notification)
        
        val msg = mServiceHandler.obtainMessage()
        msg.arg1 = startId
        msg.obj = intent!!
        mServiceHandler.sendMessage(msg)

        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        private const val DEBUG_DONT_POST = true
        private const val NOTIFICATION_ID = 1 // must not be 0
        private const val NOTIFICATION_CHANNEL_NAME = "Main"
        private const val NOTIFICATION_CHANNEL_ID = NOTIFICATION_CHANNEL_NAME
        private const val EXTRA_POST_ID = "post_id"

        fun newIntent(context: Context, postId: Long): Intent {
            val i = Intent(context, SubmitService::class.java)
            i.putExtra(EXTRA_POST_ID, postId)
            return i
        }
        
        // assumes that not all of the arguments are true
        private fun constructErrorMessage(post: Post?,
                                          goodPost: Boolean,
                                          signedIn: Boolean,
                                          notRatelimited: Boolean,
                                          networkAvailable: Boolean,
                                          notDebugging: Boolean): String {

            val reasonsDidntPost = mutableListOf<String>()

            val postNotFoundReason = "didn't find the post in the database"
            val notSignedInReason = "wasn't signed in"
            val ratelimitedReason = "was ratelimited"
            val networkNotAvailableReason = "network wasn't available"
            val debuggingReason = "was debugging"

            if (!goodPost) reasonsDidntPost.add(postNotFoundReason)
            if (!signedIn) reasonsDidntPost.add(notSignedInReason)
            if (!notRatelimited) reasonsDidntPost.add(ratelimitedReason)
            if (!networkAvailable) reasonsDidntPost.add(networkNotAvailableReason)
            if (!notDebugging) reasonsDidntPost.add(debuggingReason)

            val postString = if (goodPost)
                "the post titled '${post!!.title}' with ID ${post.id}"
            else
                "this post"

            var errorMessage = "Couldn't submit $postString because "

            reasonsDidntPost.forEachIndexed { index, reason ->
                errorMessage += if (index == reasonsDidntPost.size - 1)
                    reason
                else
                    "$reason and "
            }
            
            return errorMessage
        }
    }
}
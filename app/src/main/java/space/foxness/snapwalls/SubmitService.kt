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

            log("I AM SUBMIT")

            // todo: add all posts that failed to be submitted to a 'failed' list
            // todo: add network safeguard to auth

            // BIG NOTE: stopSelf(msg.arg1) MUST BE CALLED BEFORE RETURNING
            // DON'T RETURN WITHOUT CALLING IT

            // this code is impossible to format to look good with this brace styling :(
            
            val intent = msg.obj as Intent
            val postId = intent.getLongExtra(EXTRA_POST_ID, -1)
            val post = Queue.getInstance(this@SubmitService).getPost(postId)
            if (post == null) {

                log("POST NOT FOUND")

            } else {

                val reddit = Autoreddit.getInstance(this@SubmitService).reddit
                if (!reddit.canSubmitRightNow) {

                    log("NOT LOGGED IN OR RATELIMITED")

                } else {

                    if (!isNetworkAvailable()) {

                        log("NETWORK NOT AVAILABLE")

                    } else {

                        if (!DEBUG_DONT_POST) {
                            reddit.submit(post, { error, link ->
                                if (error != null) {
                                    log("ERROE: ${error.message}")
                                    throw error
                                }

                                log("GOT LINK: $link")
                            })
                        }
                    }
                }
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
    }
}
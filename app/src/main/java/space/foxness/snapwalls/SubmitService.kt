package space.foxness.snapwalls

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log


class SubmitService : Service() {
    
    private lateinit var mServiceLooper: Looper
    private lateinit var mServiceHandler: ServiceHandler

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        
        override fun handleMessage(msg: Message) {

            val intent = msg.obj as Intent
            val postId = intent.getLongExtra(EXTRA_POST_ID, -1)
            Log.i(TAG, "I AM SUBMIT, ATTEMPTING TO POST ID $postId")

            if (DEBUG) {
                stopSelf(msg.arg1)
                return
            }

            val post = Queue.getInstance(this@SubmitService).getPost(postId)
            if (post == null) {
                Log.i(TAG, "No post to submit")
                stopSelf(msg.arg1)
                return
            }

            val reddit = Autoreddit.getInstance(this@SubmitService).reddit

            if (!reddit.canSubmitRightNow) {
                Log.i(TAG, "Can't submit right now")
                stopSelf(msg.arg1)
                return
            }

            reddit.submit(post, { error, link ->
                if (error != null) {
                    Log.i(TAG, "ERROE: ${error.message}")
                    throw error
                }

                Log.i(TAG, "GOT LINK: $link")
            })

            stopSelf(msg.arg1)
        }
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
        
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }
        
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
        private const val DEBUG = false
        private const val TAG = "SubmitService"
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
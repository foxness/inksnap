package space.foxness.snapwalls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat

// todo: different notification ids for different posts

class NotificationFactory private constructor(private val context: Context)
{
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val ids = listOf(SERVICE_CHANNEL_ID, ERROR_CHANNEL_ID, SUCCESS_CHANNEL_ID)
            val names = listOf(SERVICE_CHANNEL_NAME, ERROR_CHANNEL_NAME, SUCCESS_CHANNEL_NAME)
            val importances = listOf(SERVICE_CHANNEL_IMPORTANCE, ERROR_CHANNEL_IMPORTANCE, SUCCESS_CHANNEL_IMPORTANCE)
            
            for (i in ids.indices)
            {
                val channel = NotificationChannel(ids[i], names[i], importances[i])
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    private fun buildNotification(channelId: String, title: String, text: String): Notification
    {
        return NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(ICON)
                .build()
    }
    
    private fun notify(notificationId: Int, notification: Notification)
    {
        notificationManager.notify(notificationId, notification)
    }
    
    fun getServiceNotification(): Notification
    {
        return buildNotification(
                SERVICE_CHANNEL_ID,
                "Submitting...",
                "Your post is being submitted...")
    }
    
    fun showErrorNotification()
    {
        val notification = buildNotification(
                ERROR_CHANNEL_ID,
                "An error has occurred",
                "An error has occurred while submitting")
        
        notify(ERROR_NOTIFICATION_ID, notification)
    }
    
    // todo: open post link upon clicking on the notification
    fun showSuccessNotification(postTitle: String)
    {
        val notification = buildNotification(
                SUCCESS_CHANNEL_ID,
                "Your post has been submitted",
                "Your post \"$postTitle\" has succesfully been submitted")

        notify(SUCCESS_NOTIFICATION_ID, notification)
    }
    
    companion object : SingletonHolder<NotificationFactory, Context>(::NotificationFactory)
    {
        private const val ICON = R.drawable.snapwalls_icon
        
        const val SERVICE_NOTIFICATION_ID = 1 // must not be 0
        private const val ERROR_NOTIFICATION_ID = 2
        private const val SUCCESS_NOTIFICATION_ID = 3
        
        private const val SERVICE_CHANNEL_NAME = "Autosubmit"
        private const val ERROR_CHANNEL_NAME = "Failed post"
        private const val SUCCESS_CHANNEL_NAME = "Sucessful post"
        
        private const val SERVICE_CHANNEL_ID = "service"
        private const val ERROR_CHANNEL_ID = "error"
        private const val SUCCESS_CHANNEL_ID = "success"
        
        @RequiresApi(Build.VERSION_CODES.N)
        private const val SERVICE_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT
        @RequiresApi(Build.VERSION_CODES.N)
        private const val ERROR_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT
        @RequiresApi(Build.VERSION_CODES.N)
        private const val SUCCESS_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT
    }
}
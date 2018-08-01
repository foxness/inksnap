package space.foxness.snapwalls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat

// todo: different notification ids for different posts

class NotificationFactory private constructor(private val context: Context)
{
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    fun createNotificationChannels() // should be run on first launch
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
    
    private fun prebuildNotification(channelId: String, title: String, text: String): NotificationCompat.Builder
    {
        return NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(ICON)
    }
    
    private fun notify(notificationId: Int, notification: Notification)
    {
        notificationManager.notify(notificationId, notification)
    }
    
    fun getServiceNotification(): Notification
    {
        return prebuildNotification(
                SERVICE_CHANNEL_ID,
                "Submitting...", // todo: extract
                "Your post is being submitted...").build()
    }
    
    fun showErrorNotification()
    {
        val notification = prebuildNotification(
                ERROR_CHANNEL_ID,
                "An error has occurred", // todo: extract
                "An error has occurred while submitting").build()
        
        notify(ERROR_NOTIFICATION_ID, notification)
    }
    
    // todo: open post link upon clicking on the notification
    fun showSuccessNotification(postTitle: String)
    {
        val intent = QueueActivity.newIntent(context)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        
        // todo: maybe add PendingIntent.FLAG_UPDATE_CURRENT to flags?
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val notification = prebuildNotification(
                SUCCESS_CHANNEL_ID,
                "Your post has been submitted", // todo: extract
                "Your post \"$postTitle\" has succesfully been submitted")
                .setContentIntent(pendingIntent)
                .build()

        notify(SUCCESS_NOTIFICATION_ID, notification)
    }
    
    companion object : SingletonHolder<NotificationFactory, Context>(::NotificationFactory)
    {
        private const val ICON = R.drawable.snapwalls_icon
        
        const val SERVICE_NOTIFICATION_ID = 1 // must not be 0
        private const val ERROR_NOTIFICATION_ID = 2
        private const val SUCCESS_NOTIFICATION_ID = 3
        
        private const val SERVICE_CHANNEL_NAME = "Autosubmit" // todo: extract
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
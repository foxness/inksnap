package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter

class StartupReceiver : BroadcastReceiver()
{
    @SuppressLint("UnsafeProtectedBroadcastReceiver") // todo: handle this
    override fun onReceive(context: Context, intent: Intent)
    {
        val settingsManager = SettingsManager.getInstance(context)
        val log = Log.getInstance(context)
        val postScheduler = PostScheduler.getInstance(context)
        
        log.log("Log receiver has awoken")
        
        if (settingsManager.autosubmitEnabled)
        {
            log.log("Autosubmit is enabled")
            
            val scheduled = postScheduler.isServiceScheduled()
            log.log("Is autosubmit service already scheduled? $scheduled")
            
            if (!scheduled)
            {
                try
                {
                    postScheduler.scheduleServiceForNextPost()
                    log.log("Successfully scheduled the service for the next post")
                }
                catch (ex: Exception)
                {
                    val errors = StringWriter()
                    ex.printStackTrace(PrintWriter(errors))
                    val stacktrace = errors.toString()

                    val errorMsg = "AN EXCEPTION HAS OCCURED. STACKTRACE:\n$stacktrace"
                    log.log(errorMsg)
                }
            }
            else
            {
                log.log("Going back to sleep")
            }
        }
        else
        {
            log.log("Autosubmit is not enabled, going back to sleep")
        }
    }
}
package me.nocturnl.inksnap

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class LauncherActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val settingsManager = SettingsManager.getInstance(this)
        
        if (!settingsManager.notFirstLaunch)
        {
            settingsManager.initializeDefaultSettings()
            NotificationFactory.getInstance(this).createNotificationChannels()
            settingsManager.notFirstLaunch = true
        }

        val intent = if (settingsManager.firstLaunchCourseCompleted)
        {
            MainActivity.newIntent(this)
        }
        else
        {
            FirstLaunchCourseActivity.newIntent(this)
        }
        
        startActivity(intent)
        finish()
    }
}
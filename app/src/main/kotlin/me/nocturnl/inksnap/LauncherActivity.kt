package me.nocturnl.inksnap

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class LauncherActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val settingsManager = SettingsManager.getInstance(this)
        
        val intent: Intent
        if (!settingsManager.notFirstLaunch)
        {
            settingsManager.initializeDefaultSettings()
            NotificationFactory.getInstance(this).createNotificationChannels()
            settingsManager.notFirstLaunch = true
            
            intent = FirstLaunchCourseActivity.newIntent(this)
        }
        else
        {
            intent = MainActivity.newIntent(this)
        }
        
        startActivity(intent)
        finish()
    }
}
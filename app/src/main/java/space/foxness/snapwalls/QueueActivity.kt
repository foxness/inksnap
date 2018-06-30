package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import space.foxness.snapwalls.Util.log

// todo: replace fragments in onStart if type has changed

class QueueActivity : AppCompatActivity()
{
    private lateinit var settingsManager: SettingsManager
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        
        log("onCreate()")
        
        setContentView(SINGLE_FRAGMENT_LAYOUT)

        settingsManager = SettingsManager.getInstance(this)

        val fm = supportFragmentManager
        var fragment: Fragment? = fm.findFragmentById(FRAGMENT_CONTAINER)

        if (fragment == null)
        {
            fragment = createFragment()
            fm.beginTransaction().add(FRAGMENT_CONTAINER, fragment).commit()
        }
    }
    
    private fun createFragment(): QueueFragment
    {
        log("Created a new fragment")
        return when (settingsManager.autosubmitType)
        {
            SettingsManager.AutosubmitType.Manual -> ManualQueueFragment()
            SettingsManager.AutosubmitType.Periodic -> PeriodicQueueFragment()
        }
    }

    companion object
    {
        private const val SINGLE_FRAGMENT_LAYOUT = R.layout.activity_fragment
        private const val FRAGMENT_CONTAINER = R.id.fragment_container
    }
}

package space.foxness.snapwalls

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity()
{
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var fragmentManager: FragmentManager
    
    private lateinit var settingsManager: SettingsManager
    
    private var currentFragment: Fragment? = null
    
    private var selectedItemId = HOME_ITEM_ID
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsManager = SettingsManager.getInstance(this)
        if (!settingsManager.notFirstLaunch)
        {
            settingsManager.initializeDefaultSettings()
            NotificationFactory.getInstance(this).createNotificationChannels()
            settingsManager.notFirstLaunch = true
        }
        
        fragmentManager = supportFragmentManager
        
        bottomNavigation = findViewById(R.id.main_bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener {
            val itemId = it.itemId
            selectFragment(itemId)
            true
        }
        
        val itemId = savedInstanceState?.getInt(ARG_SELECTED_ITEM_ID) ?: HOME_ITEM_ID
        selectFragment(itemId)
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        outState.putInt(ARG_SELECTED_ITEM_ID, selectedItemId)
        super.onSaveInstanceState(outState)
    }

    override fun onStart()
    {
        if (selectedItemId == HOME_ITEM_ID)
        {
            val currentType = when (currentFragment)
            {
                is ManualQueueFragment -> SettingsManager.AutosubmitType.Manual
                is PeriodicQueueFragment -> SettingsManager.AutosubmitType.Periodic
                else -> throw Exception("how?")
            }
            
            if (currentType != settingsManager.autosubmitType)
            {
                val newFragment = getQueueFragment()

                fragmentManager.beginTransaction()
                        .replace(FRAGMENT_CONTAINER, newFragment)
                        .commit()
                
                currentFragment = newFragment
            }
        }
        
        super.onStart()
    }
    
    private fun getQueueFragment(): QueueFragment
    {
        return when (settingsManager.autosubmitType)
        {
            SettingsManager.AutosubmitType.Manual -> ManualQueueFragment.newInstance()
            SettingsManager.AutosubmitType.Periodic -> PeriodicQueueFragment.newInstance()
        }
    }

    private fun selectFragment(itemId: Int)
    {
        selectedItemId = itemId // todo: dont change fragment if the item id didnt change
        
        val newFragment = when (selectedItemId)
        {
            R.id.action_queue -> getQueueFragment()
            R.id.action_posted -> PostedFragment.newInstance()
            R.id.action_failed -> FailedFragment.newInstance()
            
            else -> throw Exception("what")
        }
        
        if (currentFragment == null)
        {
            fragmentManager.beginTransaction()
                    .add(FRAGMENT_CONTAINER, newFragment)
                    .commit()
        }
        else
        {
            fragmentManager.beginTransaction()
                    .replace(FRAGMENT_CONTAINER, newFragment)
                    .commit()
        }
        
        currentFragment = newFragment
    }

    companion object
    {
        private const val ARG_SELECTED_ITEM_ID = "arg_selected_item_id"
        private const val HOME_ITEM_ID = R.id.action_queue
        private const val FRAGMENT_CONTAINER = R.id.main_fragment_container
        
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
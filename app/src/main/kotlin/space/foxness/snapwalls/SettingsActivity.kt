package space.foxness.snapwalls

import android.content.Context
import android.content.Intent

class SettingsActivity : SingleFragmentActivity()
{
    override fun createFragment() = SettingsFragment.newInstance()
    
    companion object
    {
        fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
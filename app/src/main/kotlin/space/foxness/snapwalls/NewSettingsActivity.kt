package space.foxness.snapwalls

import android.content.Context
import android.content.Intent

class NewSettingsActivity : SingleFragmentActivity()
{
    override fun createFragment() = NewSettingsFragment()
    
    companion object
    {
        fun newIntent(context: Context) = Intent(context, NewSettingsActivity::class.java)
    }
}
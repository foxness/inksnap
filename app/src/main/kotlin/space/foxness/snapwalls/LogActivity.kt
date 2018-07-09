package space.foxness.snapwalls

import android.content.Context
import android.content.Intent

class LogActivity : SingleFragmentActivity()
{
    override fun createFragment() = LogFragment.newInstance()
    
    companion object
    {
        fun newIntent(packageContext: Context): Intent
        {
            return Intent(packageContext, LogActivity::class.java)
        }
    }
}
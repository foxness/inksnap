package space.foxness.snapwalls

import android.content.Context
import android.content.Intent

class NewpostActivity : SingleFragmentActivity()
{
    override fun createFragment() = NewpostFragment.newInstance()
    
    companion object
    {
        fun newIntent(context: Context) = Intent(context, NewpostActivity::class.java)
    }
}
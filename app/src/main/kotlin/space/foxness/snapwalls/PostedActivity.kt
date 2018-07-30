package space.foxness.snapwalls

import android.content.Context
import android.content.Intent

class PostedActivity : SingleFragmentActivity()
{
    override fun createFragment() = PostedFragment()
    
    companion object
    {
        fun newIntent(context: Context) = Intent(context, PostedActivity::class.java)
    }
}
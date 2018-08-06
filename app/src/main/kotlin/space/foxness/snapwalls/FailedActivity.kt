package space.foxness.snapwalls

import android.content.Context
import android.content.Intent

class FailedActivity : SingleFragmentActivity()
{
    override fun createFragment() = FailedFragment.newInstance()

    companion object
    {
        fun newIntent(context: Context) = Intent(context, FailedActivity::class.java)
    }
}
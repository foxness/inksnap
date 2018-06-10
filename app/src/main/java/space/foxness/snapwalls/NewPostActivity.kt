package space.foxness.snapwalls

import android.content.Context
import android.content.Intent

class NewPostActivity : SingleFragmentActivity() {
    override fun createFragment() = PostFragment.newInstance(null,  true)
    
    companion object {
        fun newIntent(context: Context) = Intent(context, NewPostActivity::class.java) // todo: add allowScheduledDateEditing
    }
}
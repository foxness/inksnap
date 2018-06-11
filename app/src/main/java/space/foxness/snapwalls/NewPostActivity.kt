package space.foxness.snapwalls

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment

class NewPostActivity : SingleFragmentActivity() {
    
    override fun createFragment(): Fragment {
        val allowScheduledDateEditing = intent.getBooleanExtra(EXTRA_ALLOW_SCHEDULED_DATE_EDITING, false)
        return PostFragment.newInstance(null,  allowScheduledDateEditing)
    }
    
    companion object {
        private const val EXTRA_ALLOW_SCHEDULED_DATE_EDITING = "allow_scheduled_date_editing"

        fun newIntent(packageContext: Context, allowScheduledDateEditing: Boolean): Intent {

            val i = Intent(packageContext, NewPostActivity::class.java)
            i.putExtra(EXTRA_ALLOW_SCHEDULED_DATE_EDITING, allowScheduledDateEditing)
            return i
        }
    }
}
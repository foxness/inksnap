package space.foxness.snapwalls

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment

class NewPostActivity : SingleFragmentActivity() {
    
    override fun createFragment(): Fragment {
        val allowIntendedSubmitDateEditing = intent.getBooleanExtra(EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING, false)
        return PostFragment.newInstance(null,  allowIntendedSubmitDateEditing)
    }
    
    companion object {
        private const val EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING = "allow_intended_submit_date_editing"

        fun newIntent(packageContext: Context, allowIntendedSubmitDateEditing: Boolean): Intent {

            val i = Intent(packageContext, NewPostActivity::class.java)
            i.putExtra(EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING, allowIntendedSubmitDateEditing)
            return i
        }
    }
}
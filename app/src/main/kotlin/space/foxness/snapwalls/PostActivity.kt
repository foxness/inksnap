package space.foxness.snapwalls

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment

class PostActivity : SingleFragmentActivity()
{
    override fun createFragment(): Fragment
    {
        val i = intent

        val allowIntendedSubmitDateEditing =
                i.getBooleanExtra(EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING, false)

        val post = if (i.getBooleanExtra(EXTRA_NEW_POST, false))
        {
            null
        }
        else
        {
            i.getSerializableExtra(EXTRA_POST) as Post
        }

        return PostFragment.newInstance(post, allowIntendedSubmitDateEditing)
    }
    
    companion object
    {
        private const val EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING = "aisde"
        private const val EXTRA_NEW_POST = "new_post"
        private const val EXTRA_POST = "post"

        const val RESULT_CODE_DELETED = PostFragment.RESULT_CODE_DELETED

        fun getPostFromResult(data: Intent) = PostFragment.getPostFromResult(data)

        fun getDeletedPostIdFromResult(data: Intent) = PostFragment.getDeletedPostIdFromResult(data)
        
        fun newIntent(packageContext: Context, post: Post?, allowIntendedSubmitDateEditing: Boolean): Intent
        {
            val i = Intent(packageContext, PostActivity::class.java)
            i.putExtra(EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING, allowIntendedSubmitDateEditing)

            if (post == null)
            {
                i.putExtra(EXTRA_NEW_POST, true)
            }
            else
            {
                i.putExtra(EXTRA_NEW_POST, false)
                i.putExtra(EXTRA_POST, post)
            }

            return i
        }
    }
}
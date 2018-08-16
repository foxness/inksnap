package space.foxness.snapwalls

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.MenuItem

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        if (item.itemId == android.R.id.home)
        {
            showDiscardDialogIfNeeded {
                // maybe it should be this? v
                // navigateUpTo(MainActivity.newIntent(this))
                
                finish()
            }
            
            return true
        }
        
        return super.onOptionsItemSelected(item)
    }
    
    private fun showDiscardDialogIfNeeded(onDiscard: () -> Unit)
    {
        val postFragment = supportFragmentManager.findFragmentById(FRAGMENT_CONTAINER) as PostFragment

        if (postFragment.areThereUnsavedChanges())
        {
            val onDiscardInternal = { dialogInterface: DialogInterface, which: Int ->
                onDiscard()
            }

            val onCancel = { dialogInterface: DialogInterface, which: Int ->
                dialogInterface.cancel()
            }

            // todo: extract
            val msg = if (postFragment.newPost) "Discard the post?" else "Discard the changes?"

            val dialog = AlertDialog.Builder(this)
                    .setMessage(msg)
                    .setPositiveButton("Discard", onDiscardInternal) // todo: extract
                    .setNegativeButton(android.R.string.cancel, onCancel)
                    .create()

            dialog.show()
        }
        else
        {
            onDiscard()
        }
    }

    override fun onBackPressed()
    {
        showDiscardDialogIfNeeded {
            super.onBackPressed()
        }
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
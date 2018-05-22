package space.foxness.snapwalls

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log

class SubmitService : IntentService(TAG) {
    
    override fun onHandleIntent(intent: Intent?) {
        
        val postId = intent!!.getLongExtra(EXTRA_POST_ID, -1)
        Log.i(TAG, "I AM SUBMIT, ATTEMPTING TO POST ID $postId")

        if (DEBUG)
            return
        
        val post = Queue.getInstance(applicationContext).getPost(postId)
        if (post == null) {
            Log.i(TAG, "No post to submit")
            return
        }
        
        val reddit = Autoreddit.getInstance(applicationContext).reddit
        
        if (!reddit.canSubmitRightNow) {
            Log.i(TAG, "Can't submit right now")
            return
        }

        reddit.submit(post, { error, link ->
            if (error != null) {
                Log.i(TAG, "ERROE: ${error.message}")
                throw error
            }

            Log.i(TAG, "GOT LINK: $link")
        })
    }

    companion object {
        private const val DEBUG = true
        
        private const val TAG = "SubmitService"
        private const val EXTRA_POST_ID = "post_id"
        
        fun newIntent(context: Context, postId: Long): Intent {
            val i = Intent(context, SubmitService::class.java)
            i.putExtra(EXTRA_POST_ID, postId)
            return i
        }
    }
}

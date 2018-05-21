package space.foxness.snapwalls

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log

class SubmitService : IntentService(TAG) {
    
    override fun onHandleIntent(intent: Intent?) {
        
        val postId = intent?.getLongExtra(EXTRA_POST_ID, -1)!!
        Log.i(TAG, "I AM SUBMIT, ID TO POST: $postId")

        val post = Queue.getInstance(applicationContext).getPost(postId)
//        if (post == null)
//        {
//            Log.i(TAG, "No post to submit")
//            return
//        }
        
        val reddit = Reddit(object: Reddit.Callbacks {
            
            override fun onNewAccessToken() {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onNewRefreshToken() {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onNewLastSubmissionDate() {
                // todo: implement
            }
        })
        
        val config = Config(applicationContext)

        reddit.accessToken = config.accessToken
        reddit.refreshToken = config.refreshToken
        reddit.accessTokenExpirationDate = config.accessTokenExpirationDate
        reddit.lastSubmissionDate = config.lastSubmissionDate
        
        if (!reddit.canSubmitRightNow) {
            Log.i(TAG, "Can't submit right now")
            return
        }

        reddit.submit(post, { error, link ->
            if (error != null)
            {
                Log.i(TAG, "ERROE: ${error.message}")
                throw error
            }

            Log.i(TAG, "GOT LINK: $link")
        })
    }

    companion object {
        private const val TAG = "SubmitService"
        private const val EXTRA_POST_ID = "post_id"
        
        fun newIntent(context: Context, postId: Long): Intent {
            return Intent(context, SubmitService::class.java).putExtra(EXTRA_POST_ID, postId)
        }
    }
}

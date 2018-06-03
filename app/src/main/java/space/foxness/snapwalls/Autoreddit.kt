package space.foxness.snapwalls

import android.content.Context

class Autoreddit private constructor(context: Context) {

    private val config = Config.getInstance(context)
    
    private val saver = Saver()

    val reddit = Reddit.getInstance(saver)
    
    init {
        restoreConfig()
    }

    private fun restoreConfig() {
        reddit.accessToken = config.redditAccessToken
        reddit.refreshToken = config.redditRefreshToken
        reddit.accessTokenExpirationDate = config.redditAccessTokenExpirationDate
        reddit.lastSubmissionDate = config.redditLastSubmissionDate
    }

    private inner class Saver : Reddit.Callbacks {
        
        override fun onNewAccessToken() {
            config.redditAccessToken = reddit.accessToken
            config.redditAccessTokenExpirationDate = reddit.accessTokenExpirationDate
        }

        override fun onNewRefreshToken() {
            config.redditRefreshToken = reddit.refreshToken
        }

        override fun onNewLastSubmissionDate() {
            config.redditLastSubmissionDate = reddit.lastSubmissionDate
        }
    }
    
    companion object : SingletonHolder<Autoreddit, Context>(::Autoreddit)
}
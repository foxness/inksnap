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
        reddit.accessToken = config.accessToken
        reddit.refreshToken = config.refreshToken
        reddit.accessTokenExpirationDate = config.accessTokenExpirationDate
        reddit.lastSubmissionDate = config.lastSubmissionDate
    }

    private inner class Saver : Reddit.Callbacks {
        
        override fun onNewAccessToken() {
            config.accessToken = reddit.accessToken
            config.accessTokenExpirationDate = reddit.accessTokenExpirationDate
        }

        override fun onNewRefreshToken() {
            config.refreshToken = reddit.refreshToken
        }

        override fun onNewLastSubmissionDate() {
            config.lastSubmissionDate = reddit.lastSubmissionDate
        }
    }
    
    companion object : SingletonHolder<Autoreddit, Context>(::Autoreddit)
}
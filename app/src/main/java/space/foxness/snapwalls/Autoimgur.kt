package space.foxness.snapwalls

import android.content.Context
import org.joda.time.DateTime

class Autoimgur private constructor(context: Context) {

    private val config = Config.getInstance(context)

    private val saver = Saver()

    val imgurAccount = ImgurAccount(saver)

    init {
        restoreConfig()
    }

    private fun restoreConfig() {
        imgurAccount.accessToken = config.imgurAccessToken
        imgurAccount.refreshToken = config.imgurRefreshToken
        imgurAccount.accessTokenExpirationDate = config.imgurAccessTokenExpirationDate
    }

    private inner class Saver : ImgurAccount.Callbacks {
        
        override fun onNewAccessToken(newAccessToken: String, newAccessTokenExpirationDate: DateTime) {
            config.imgurAccessToken = newAccessToken
            config.imgurAccessTokenExpirationDate = newAccessTokenExpirationDate
        }

        override fun onNewRefreshToken(newRefreshToken: String) {
            config.imgurRefreshToken = newRefreshToken
        }
    }

    companion object : SingletonHolder<Autoimgur, Context>(::Autoimgur)
}
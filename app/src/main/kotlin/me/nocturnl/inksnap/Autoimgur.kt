package me.nocturnl.inksnap

import android.content.Context
import org.joda.time.DateTime

class Autoimgur private constructor(context: Context)
{
    private val settingsManager = SettingsManager.getInstance(context)

    private val saver = Saver()

    val imgurAccount = ImgurAccount(saver)

    init
    {
        restoreConfig()
    }

    private fun restoreConfig()
    {
        imgurAccount.accessToken = settingsManager.imgurAccessToken
        imgurAccount.refreshToken = settingsManager.imgurRefreshToken
        imgurAccount.accessTokenExpirationDate = settingsManager.imgurAccessTokenExpirationDate
    }

    private inner class Saver : ImgurAccount.Callbacks
    {
        override fun onNewAccessToken(newAccessToken: String?,
                                      newAccessTokenExpirationDate: DateTime?)
        {
            settingsManager.imgurAccessToken = newAccessToken
            settingsManager.imgurAccessTokenExpirationDate = newAccessTokenExpirationDate
        }

        override fun onNewRefreshToken(newRefreshToken: String?)
        {
            settingsManager.imgurRefreshToken = newRefreshToken
        }
    }

    companion object : SingletonHolder<Autoimgur, Context>(::Autoimgur)
}
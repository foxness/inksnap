package space.foxness.snapwalls

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.Duration

class Config private constructor(context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    var redditAccessToken: String?
        get() = getString(CONFIG_REDDIT_ACCESS_TOKEN)
        set(value) = setString(CONFIG_REDDIT_ACCESS_TOKEN, value)

    var redditRefreshToken: String?
        get() = getString(CONFIG_REDDIT_REFRESH_TOKEN)
        set(value) = setString(CONFIG_REDDIT_REFRESH_TOKEN, value)

    var redditAccessTokenExpirationDate: DateTime?
        get() = getDateTime(CONFIG_REDDIT_ACCESS_TOKEN_EXPIRATION_DATE)
        set(value) = setDateTime(CONFIG_REDDIT_ACCESS_TOKEN_EXPIRATION_DATE, value)

    var redditLastSubmissionDate: DateTime?
        get() = getDateTime(CONFIG_REDDIT_LAST_SUBMISSION_DATE)
        set(value) = setDateTime(CONFIG_REDDIT_LAST_SUBMISSION_DATE, value)
    
    var autosubmitEnabled: Boolean
        get() = getBool(CONFIG_AUTOSUBMIT_ENABLED)
        set(value) = setBool(CONFIG_AUTOSUBMIT_ENABLED, value)
    
    var timeLeft: Duration?
        get() = getDuration(CONFIG_TIME_LEFT)
        set(value) = setDuration(CONFIG_TIME_LEFT, value)

    var imgurAccessToken: String?
        get() = getString(CONFIG_IMGUR_ACCESS_TOKEN)
        set(value) = setString(CONFIG_IMGUR_ACCESS_TOKEN, value)

    var imgurRefreshToken: String?
        get() = getString(CONFIG_IMGUR_REFRESH_TOKEN)
        set(value) = setString(CONFIG_IMGUR_REFRESH_TOKEN, value)

    var imgurAccessTokenExpirationDate: DateTime?
        get() = getDateTime(CONFIG_IMGUR_ACCESS_TOKEN_EXPIRATION_DATE)
        set(value) = setDateTime(CONFIG_IMGUR_ACCESS_TOKEN_EXPIRATION_DATE, value)

    private fun getString(key: String) = sharedPreferences.getString(key, null)
    
    private fun setString(key: String, value: String?) = sharedPreferences.edit().putString(key, value).apply()
    
    private fun getBool(key: String) = sharedPreferences.getBoolean(key, false)

    private fun setBool(key: String, value: Boolean) = sharedPreferences.edit().putBoolean(key, value).apply()

    private fun getLong(key: String): Long? {
        val long = sharedPreferences.getLong(key, LONG_NULL_SUBSTITUTE)
        return if (long == LONG_NULL_SUBSTITUTE) null else long
    }

    private fun setLong(key: String, value: Long?) {
        val long = value ?: LONG_NULL_SUBSTITUTE
        sharedPreferences.edit().putLong(key, long).apply()
    }
    
    private fun getDateTime(key: String): DateTime? {
        val millis = getLong(key)
        return if (millis == null) null else DateTime(millis)
    }

    private fun setDateTime(key: String, value: DateTime?) {
        val millis = value?.millis
        setLong(key, millis)
    }

    private fun getDuration(key: String): Duration? {
        val millis = getLong(key)
        return if (millis == null) null else Duration(millis)
    }

    private fun setDuration(key: String, value: Duration?) {
        val millis = value?.millis
        setLong(key, millis)
    }
    
    companion object : SingletonHolder<Config, Context>(::Config) {
        private const val SHARED_PREFERENCES_NAME = "config"
        private const val CONFIG_REDDIT_ACCESS_TOKEN = "redditAccessToken"
        private const val CONFIG_REDDIT_REFRESH_TOKEN = "redditRefreshToken"
        private const val CONFIG_REDDIT_ACCESS_TOKEN_EXPIRATION_DATE = "redditAccessTokenExpirationDate"
        private const val CONFIG_REDDIT_LAST_SUBMISSION_DATE = "redditLastSubmissionDate"
        private const val CONFIG_AUTOSUBMIT_ENABLED = "autosubmitEnabled"
        private const val CONFIG_TIME_LEFT = "timeLeft"
        private const val CONFIG_IMGUR_ACCESS_TOKEN = "imgurAccessToken"
        private const val CONFIG_IMGUR_REFRESH_TOKEN = "imgurRefreshToken"
        private const val CONFIG_IMGUR_ACCESS_TOKEN_EXPIRATION_DATE = "imgurAccessTokenExpirationDate"

        private const val LONG_NULL_SUBSTITUTE: Long = Long.MIN_VALUE
    }
}
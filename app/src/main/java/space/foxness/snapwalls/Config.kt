package space.foxness.snapwalls

import android.content.Context
import org.joda.time.DateTime

class Config private constructor(context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = getString(CONFIG_ACCESS_TOKEN)
        set(value) = setString(CONFIG_ACCESS_TOKEN, value)

    var refreshToken: String?
        get() = getString(CONFIG_REFRESH_TOKEN)
        set(value) = setString(CONFIG_REFRESH_TOKEN, value)

    var accessTokenExpirationDate
        get() = getDateTime(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE)
        set(value) = setDateTime(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, value)

    var lastSubmissionDate
        get() = getDateTime(CONFIG_LAST_SUBMISSION_DATE)
        set(value) = setDateTime(CONFIG_LAST_SUBMISSION_DATE, value)
    
    var scheduled
        get() = getBool(CONFIG_SCHEDULED)
        set(value) = setBool(CONFIG_SCHEDULED, value)

    private fun getString(key: String) = sharedPreferences.getString(key, null)
    
    private fun setString(key: String, value: String?) = sharedPreferences.edit().putString(key, value).apply()
    
    private fun getBool(key: String) = sharedPreferences.getBoolean(key, false)

    private fun setBool(key: String, value: Boolean) = sharedPreferences.edit().putBoolean(key, value).apply()
    
    private fun getDateTime(key: String): DateTime? {
        val dateInMs = sharedPreferences.getLong(key, CONFIG_NULL_SUBSTITUTE)
        return if (dateInMs == CONFIG_NULL_SUBSTITUTE) null else DateTime(dateInMs)
    }

    private fun setDateTime(key: String, value: DateTime?) {
        val long = value?.millis ?: CONFIG_NULL_SUBSTITUTE
        sharedPreferences.edit().putLong(key, long).apply()
    }
    
    companion object : SingletonHolder<Config, Context>(::Config) {
        private const val SHARED_PREFERENCES_NAME = "config"
        private const val CONFIG_ACCESS_TOKEN = "accessToken"
        private const val CONFIG_REFRESH_TOKEN = "refreshToken"
        private const val CONFIG_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate"
        private const val CONFIG_LAST_SUBMISSION_DATE = "lastSubmissionDate"
        private const val CONFIG_SCHEDULED = "scheduled"

        private const val CONFIG_NULL_SUBSTITUTE: Long = 0
    }
}
package space.foxness.snapwalls

import android.content.Context
import org.joda.time.Instant

class Config private constructor(context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = getString(CONFIG_ACCESS_TOKEN)
        set(value) = setString(CONFIG_ACCESS_TOKEN, value)

    var refreshToken: String?
        get() = getString(CONFIG_REFRESH_TOKEN)
        set(value) = setString(CONFIG_REFRESH_TOKEN, value)

    var accessTokenExpirationDate
        get() = getInstant(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE)
        set(value) = setInstant(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, value)

    var lastSubmissionDate
        get() = getInstant(CONFIG_LAST_SUBMISSION_DATE)
        set(value) = setInstant(CONFIG_LAST_SUBMISSION_DATE, value)


    private fun getString(field: String) = sharedPreferences.getString(field, null)
    
    private fun setString(field: String, value: String?) = sharedPreferences.edit().putString(field, value).apply()
    
    private fun getInstant(field: String): Instant? {
        val dateInMs = sharedPreferences.getLong(field, CONFIG_NULL_SUBSTITUTE)
        return if (dateInMs == CONFIG_NULL_SUBSTITUTE) null else Instant(dateInMs)
    }

    private fun setInstant(field: String, value: Instant?) {
        val long = value?.millis ?: CONFIG_NULL_SUBSTITUTE
        sharedPreferences.edit().putLong(field, long).apply()
    }
    
    companion object : SingletonHolder<Config, Context>(::Config) {
        private const val SHARED_PREFERENCES_NAME = "config"
        private const val CONFIG_ACCESS_TOKEN = "accessToken"
        private const val CONFIG_REFRESH_TOKEN = "refreshToken"
        private const val CONFIG_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate"
        private const val CONFIG_LAST_SUBMISSION_DATE = "lastSubmissionDate"

        private const val CONFIG_NULL_SUBSTITUTE: Long = 0
    }
}
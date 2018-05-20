package space.foxness.snapwalls

import android.content.Context
import java.util.*

class Config(context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = getString(CONFIG_ACCESS_TOKEN)
        set(value) = setString(CONFIG_ACCESS_TOKEN, value)

    var refreshToken: String?
        get() = getString(CONFIG_REFRESH_TOKEN)
        set(value) = setString(CONFIG_REFRESH_TOKEN, value)

    var accessTokenExpirationDate
        get() = getDate(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE)
        set(value) = setDate(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, value)

    var lastSubmissionDate
        get() = getDate(CONFIG_LAST_SUBMISSION_DATE)
        set(value) = setDate(CONFIG_LAST_SUBMISSION_DATE, value)


    private fun getString(field: String) = sharedPreferences.getString(field, null)
    
    private fun setString(field: String, value: String?) = sharedPreferences.edit().putString(field, value).apply()
    
    private fun getDate(field: String): Date? {
        val dateInMs: Long = sharedPreferences.getLong(field, CONFIG_NULL_SUBSTITUTE)
        return if (dateInMs == CONFIG_NULL_SUBSTITUTE) null else Date(dateInMs)
    }

    private fun setDate(field: String, value: Date?) {
        val long = value?.time ?: CONFIG_NULL_SUBSTITUTE
        sharedPreferences.edit().putLong(field, long).apply()
    }
    
    companion object {
        private const val SHARED_PREFERENCES_NAME = "config"
        private const val CONFIG_ACCESS_TOKEN = "accessToken"
        private const val CONFIG_REFRESH_TOKEN = "refreshToken"
        private const val CONFIG_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate"
        private const val CONFIG_LAST_SUBMISSION_DATE = "lastSubmissionDate"

        private const val CONFIG_NULL_SUBSTITUTE: Long = 0
    }
}
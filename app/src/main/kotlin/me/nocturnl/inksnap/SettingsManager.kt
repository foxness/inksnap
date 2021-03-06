package me.nocturnl.inksnap

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.Duration

class SettingsManager private constructor(context_: Context)
{
    // these do not include Settings that are set by user in SettingsActivity
    // those settings are set to default by a different method
    fun initializeDefaultSettings()
    {
        redditAccessToken = null
        redditRefreshToken = null
        redditAccessTokenExpirationDate = null
        redditLastSubmissionDate = null
        redditName = null
        submissionEnabled = false
        timeLeft = Duration.standardHours(3)
        imgurAccessToken = null
        imgurRefreshToken = null
        imgurAccessTokenExpirationDate = null
        sortBy = SortBy.Date
        notFirstLaunch = false
        schedulingType = SchedulingType.Manual
        period = Duration.standardHours(3)
        notificationIdCounter = NotificationFactory.INITIAL_NOTIFICATION_ID
        developerOptionsUnlocked = false
        firstLaunchCourseCompleted = false
    }
    
    private val sharedPreferences = context_.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var redditAccessToken: String?
        get() = getString(PREF_REDDIT_ACCESS_TOKEN)
        set(value) = setString(PREF_REDDIT_ACCESS_TOKEN, value)

    var redditRefreshToken: String?
        get() = getString(PREF_REDDIT_REFRESH_TOKEN)
        set(value) = setString(PREF_REDDIT_REFRESH_TOKEN, value)

    var redditAccessTokenExpirationDate: DateTime?
        get() = getDateTime(PREF_REDDIT_ACCESS_TOKEN_EXPIRATION_DATE)
        set(value) = setDateTime(PREF_REDDIT_ACCESS_TOKEN_EXPIRATION_DATE, value)

    var redditLastSubmissionDate: DateTime?
        get() = getDateTime(PREF_REDDIT_LAST_SUBMISSION_DATE)
        set(value) = setDateTime(PREF_REDDIT_LAST_SUBMISSION_DATE, value)

    var redditName: String?
        get() = getString(PREF_REDDIT_NAME)
        set(value) = setString(PREF_REDDIT_NAME, value)

    var submissionEnabled: Boolean
        get() = getBool(PREF_SUBMISSION_ENABLED)
        set(value) = setBool(PREF_SUBMISSION_ENABLED, value)

    var timeLeft: Duration? // todo: maybe make it non nullable?
        get() = getDuration(PREF_TIME_LEFT)
        set(value) = setDuration(PREF_TIME_LEFT, value)

    var imgurAccessToken: String?
        get() = getString(PREF_IMGUR_ACCESS_TOKEN)
        set(value) = setString(PREF_IMGUR_ACCESS_TOKEN, value)

    var imgurRefreshToken: String?
        get() = getString(PREF_IMGUR_REFRESH_TOKEN)
        set(value) = setString(PREF_IMGUR_REFRESH_TOKEN, value)

    var imgurAccessTokenExpirationDate: DateTime?
        get() = getDateTime(PREF_IMGUR_ACCESS_TOKEN_EXPIRATION_DATE)
        set(value) = setDateTime(PREF_IMGUR_ACCESS_TOKEN_EXPIRATION_DATE, value)

    enum class SortBy
    { Title, Date }

    var sortBy: SortBy
        get() = SortBy.values()[getInt(PREF_SORT_BY)!!]
        set(value) = setInt(PREF_SORT_BY, value.ordinal)
    
    var notFirstLaunch: Boolean // getBool()'s default value should be false for this to work
        get() = getBool(PREF_NOT_FIRST_LAUNCH)
        set(value) = setBool(PREF_NOT_FIRST_LAUNCH, value)

    enum class SchedulingType
    { Manual, Periodic }

    var schedulingType: SchedulingType
        get() = SchedulingType.values()[getInt(PREF_SCHEDULING_TYPE)!!]
        set(value) = setInt(PREF_SCHEDULING_TYPE, value.ordinal)
    
    var period: Duration
        get() = getDuration(PREF_PERIOD)!!
        set(value) = setDuration(PREF_PERIOD, value)

    var wallpaperMode: Boolean
        get() = getBool(PREF_WALLPAPER_MODE)
        set(value) = setBool(PREF_WALLPAPER_MODE, value)

    var notificationIdCounter: Int
        get() = getInt(PREF_NOTIFICATION_ID_COUNTER)!!
        set(value) = setInt(PREF_NOTIFICATION_ID_COUNTER, value)

    var developerOptionsUnlocked: Boolean
        get() = getBool(PREF_DEVELOPER_OPTIONS_UNLOCKED)
        set(value) = setBool(PREF_DEVELOPER_OPTIONS_UNLOCKED, value)

    var firstLaunchCourseCompleted: Boolean
        get() = getBool(PREF_FIRST_LAUNCH_COURSE_COMPLETED)
        set(value) = setBool(PREF_FIRST_LAUNCH_COURSE_COMPLETED, value)

    private fun getString(key: String) = sharedPreferences.getString(key, null)

    private fun setString(key: String, value: String?) =
            sharedPreferences.edit().putString(key, value).apply()

    private fun getBool(key: String) = sharedPreferences.getBoolean(key, false)

    private fun setBool(key: String, value: Boolean) =
            sharedPreferences.edit().putBoolean(key, value).apply()

    private fun getLong(key: String): Long?
    {
        val long = sharedPreferences.getLong(key, LONG_NULL_SUBSTITUTE)
        return if (long == LONG_NULL_SUBSTITUTE) null else long
    }

    private fun setLong(key: String, value: Long?)
    {
        val long = value ?: LONG_NULL_SUBSTITUTE
        sharedPreferences.edit().putLong(key, long).apply()
    }

    private fun getInt(key: String): Int?
    {
        val int = sharedPreferences.getInt(key, INT_NULL_SUBSTITUTE)
        return if (int == INT_NULL_SUBSTITUTE) null else int
    }

    private fun setInt(key: String, value: Int?)
    {
        val int = value ?: INT_NULL_SUBSTITUTE
        sharedPreferences.edit().putInt(key, int).apply()
    }

    private fun getDateTime(key: String): DateTime?
    {
        val millis = getLong(key)
        return if (millis == null) null else DateTime(millis)
    }

    private fun setDateTime(key: String, value: DateTime?)
    {
        val millis = value?.millis
        setLong(key, millis)
    }

    private fun getDuration(key: String): Duration?
    {
        val millis = getLong(key)
        return if (millis == null) null else Duration(millis)
    }

    private fun setDuration(key: String, value: Duration?)
    {
        val millis = value?.millis
        setLong(key, millis)
    }

    companion object : SingletonHolder<SettingsManager, Context>(::SettingsManager)
    {
        private const val NAME = "settings"
        
        // todo: separate preferences and other stored values
        private const val PREF_REDDIT_ACCESS_TOKEN = "redditAccessToken"
        private const val PREF_REDDIT_REFRESH_TOKEN = "redditRefreshToken"
        private const val PREF_REDDIT_ACCESS_TOKEN_EXPIRATION_DATE =
                "redditAccessTokenExpirationDate"
        private const val PREF_REDDIT_LAST_SUBMISSION_DATE = "redditLastSubmissionDate"
        private const val PREF_REDDIT_NAME = "redditName"
        private const val PREF_SUBMISSION_ENABLED = "submissionEnabled"
        private const val PREF_TIME_LEFT = "timeLeft"
        private const val PREF_IMGUR_ACCESS_TOKEN = "imgurAccessToken"
        private const val PREF_IMGUR_REFRESH_TOKEN = "imgurRefreshToken"
        private const val PREF_IMGUR_ACCESS_TOKEN_EXPIRATION_DATE = "imgurAccessTokenExpirationDate"
        private const val PREF_SORT_BY = "sortBy"
        private const val PREF_NOT_FIRST_LAUNCH = "notFirstLaunch"
        private const val PREF_SCHEDULING_TYPE = "schedulingType"
        private const val PREF_PERIOD = "period"
        private const val PREF_WALLPAPER_MODE = "wallpaperMode"
        private const val PREF_NOTIFICATION_ID_COUNTER = "notificationIdCounter"
        private const val PREF_DEVELOPER_OPTIONS_UNLOCKED = "developerOptionsUnlocked"
        private const val PREF_FIRST_LAUNCH_COURSE_COMPLETED = "firstLaunchCourseCompleted"

        private const val LONG_NULL_SUBSTITUTE = Long.MIN_VALUE
        private const val INT_NULL_SUBSTITUTE = Int.MIN_VALUE
    }
}
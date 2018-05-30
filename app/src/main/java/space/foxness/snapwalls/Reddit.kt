package space.foxness.snapwalls

import android.net.Uri
import khttp.structures.authorization.BasicAuthorization
import org.joda.time.DateTime
import org.joda.time.Duration

class Reddit private constructor(private val callbacks: Callbacks) { // todo: use async/await

    private var authState: String? = null
    private var authCode: String? = null
    
    var accessToken: String? = null
    var refreshToken: String? = null
    var accessTokenExpirationDate: DateTime? = null
    var lastSubmissionDate: DateTime? = null

    val isSignedIn get() = refreshToken != null

    val isRestrictedByRatelimit
        get() = lastSubmissionDate != null && DateTime.now() < lastSubmissionDate!! + Duration(RATELIMIT_MS)

    val canSubmitRightNow get() = isSignedIn && !isRestrictedByRatelimit

    val authorizationUrl: String
        get() {
            authState = randomState()
            return Uri.parse(AUTORIZE_ENDPOINT).buildUpon()
                    .appendQueryParameter("client_id", APP_CLIENT_ID)
                    .appendQueryParameter("response_type", AUTH_RESPONSE_TYPE)
                    .appendQueryParameter("state", authState)
                    .appendQueryParameter("redirect_uri", APP_REDIRECT_URI)
                    .appendQueryParameter("duration", AUTH_DURATION)
                    .appendQueryParameter("scope", AUTH_SCOPE)
                    .build().toString()
        }

    interface Callbacks {
        fun onNewAccessToken()
        fun onNewRefreshToken()
        fun onNewLastSubmissionDate()
    }

    fun submit(post: Post, callback: (Throwable?, String?) -> Unit, debugDontPost: Boolean = false, resubmit: Boolean = true, sendReplies: Boolean = true) {
        
        // todo: use postfragment's definition of bad post
        if (post.title.isEmpty() || (post.type && post.content.isEmpty()) || post.subreddit.isEmpty()) {
            callback(RuntimeException("Bad post"), null)
            return
        }

        if (debugDontPost) {
            callback(null, "DEBUG: POST NOT SUBMITTED")
            return
        }

        ensureValidAccessToken({
            
            if (it != null) {
                callback(it, null)
                return@ensureValidAccessToken
            }

            val headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Authorization" to "bearer " + accessToken!!)

            val data = mapOf(
                    "api_type" to "json",
                    "kind" to if (post.type) "link" else "self",
                    "resubmit" to if (resubmit) "true" else "false",
                    "sendreplies" to if (sendReplies) "true" else "false",
                    "sr" to post.subreddit,
                    (if (post.type) "url" else "text") to post.content,
                    "title" to post.title)

            val response = khttp.post(url = SUBMIT_ENDPOINT, headers = headers, data = data)

            if (response.statusCode != 200) {
                callback(Exception("Response code: ${response.statusCode}, response: ${response}"), null)
                return@ensureValidAccessToken
            }

            val json = response.jsonObject.optJSONObject("json")!!
            val errors = json.optJSONArray("errors")!!

            if (errors.length() == 0) {
                
                lastSubmissionDate = DateTime.now()
                callbacks.onNewLastSubmissionDate()
                callback(null, json.optJSONObject("data")?.optString("url")!!)
                
            } else {
                
                val errorString = StringBuilder("You got ${errors.length()} errors: ")
                for (i in 0 until errors.length()) {
                    val error = errors.optJSONArray(i)!!
                    val name = error.optString(0)!!
                    val description = error.optString(1)!!

                    when (name) {
                        "RATELIMIT", "NO_URL" -> errorString.append(name).append(" ").append(description).append("\n")
                        else -> {
                            callback(RuntimeException("I FOUND AN UNKNOWN ERROR:\nNAME: $name\nDESCRIPTION: $description"), null)
                            return@ensureValidAccessToken
                        }
                    }
                }

                callback(RuntimeException(errorString.toString()), null)
            }
        })
    }

    private fun ensureValidAccessToken(callback: (Throwable?) -> Unit) {
        
        if (accessToken != null && accessTokenExpirationDate!! > DateTime.now()) {
            callback(null)
            return
        }

        if (refreshToken == null) {
            callback(RuntimeException("Can't update access token without refresh token"))
            return
        }

        val headers = mapOf("User-Agent" to USER_AGENT)

        val data = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken!!)

        val auth = BasicAuthorization(APP_CLIENT_ID, APP_CLIENT_SECRET)

        val response = khttp.post(url = ACCESS_TOKEN_ENDPOINT, headers = headers, data = data, auth = auth)

        if (response.statusCode != 200) {
            callback(Exception("Response code: ${response.statusCode}, response: ${response}"))
            return
        }

        val json = response.jsonObject

        try {
            // TODO: compare received scope with intended scope?
            updateAccessToken(json.getString("access_token"), json.getInt("expires_in"))
        } catch (e: Exception) {
            callback(e)
            return
        }

        callback(null)
    }

    private fun updateAccessToken(newAccessToken: String, expiresIn: Int) {
        accessToken = newAccessToken
        accessTokenExpirationDate = DateTime.now() + Duration.standardSeconds(expiresIn.toLong())
        
        callbacks.onNewAccessToken()
    }

    fun fetchAuthTokens() {
        if (authCode == null)
            throw Exception("Can't fetch auth tokens without auth code")

        val headers = mapOf("User-Agent" to USER_AGENT)

        val data = mapOf(
                "grant_type" to "authorization_code",
                "code" to authCode,
                "redirect_uri" to APP_REDIRECT_URI)

        val auth = BasicAuthorization(APP_CLIENT_ID, APP_CLIENT_SECRET)
        
        val response = khttp.post(url = ACCESS_TOKEN_ENDPOINT, headers = headers, data = data, auth = auth)

        if (response.statusCode != 200)
            throw Exception("Response code: ${response.statusCode}, response: $response")
        
        val json = response.jsonObject

        // TODO: compare received scope with intended scope?
        refreshToken = json.getString("refresh_token")
        val accessToken = json.getString("access_token")
        val expiresIn = json.getInt("expires_in")
        
        updateAccessToken(accessToken, expiresIn)

        callbacks.onNewRefreshToken()
    }

    fun tryExtractCode(url: String): Boolean {
        if (!url.startsWith(APP_REDIRECT_URI) || authState == null)
            return false

        val uri = Uri.parse(url)
        val state = uri.getQueryParameter("state")
        if (state == null || state != authState)
            return false

        authState = null
        authCode = uri.getQueryParameter("code")
        return authCode != null // 'true' denotes success
    }

    companion object : SingletonHolder<Reddit, Callbacks>(::Reddit) {
        
        private const val APP_CLIENT_ID = "hSDlAP9u4cEFFA"
        private const val APP_CLIENT_SECRET = "" // installed apps have no secrets
        private const val APP_REDIRECT_URI = "http://localhost"

        private const val AUTORIZE_ENDPOINT = "https://www.reddit.com/api/v1/authorize.compact"
        private const val ACCESS_TOKEN_ENDPOINT = "https://www.reddit.com/api/v1/access_token"
        private const val SUBMIT_ENDPOINT = "https://oauth.reddit.com/api/submit"

        private const val AUTH_DURATION = "permanent"
        private const val AUTH_SCOPE = "submit"
        private const val AUTH_RESPONSE_TYPE = "code"
        private const val USER_AGENT = "Snapwalls by /u/foxneZz"

        private const val RATELIMIT_MS: Long = 10 * 60 * 1000 // 10 minutes, required to be long

        private fun randomState(): String { // TODO: implement this method
            return "testy"
        }
    }
}

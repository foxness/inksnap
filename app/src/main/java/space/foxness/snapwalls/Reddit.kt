package space.foxness.snapwalls

import android.net.Uri

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams

import org.json.JSONException
import org.json.JSONObject

import java.util.Calendar
import java.util.Date

import cz.msebera.android.httpclient.Header

class Reddit(private val callbacks: Callbacks) {

    private var authState: String? = null
    private var authCode: String? = null
    
    var accessToken: String? = null
    var refreshToken: String? = null
    var accessTokenExpirationDate: Date? = null
    var lastSubmissionDate: Date? = null

    val isSignedIn get() = refreshToken != null

    val isRestrictedByRatelimit get() = lastSubmissionDate != null && Date().time < lastSubmissionDate!!.time + RATELIMIT

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

    fun submit(post: Post, callback: (Throwable?, String?) -> Unit, resubmit: Boolean = true, sendReplies: Boolean = true) {
        
        if (post.title.isEmpty() || post.content.isEmpty() || post.subreddit.isEmpty())
        {
            callback(RuntimeException("Bad post"), null)
            return
        }

        ensureValidAccessToken({
            if (it != null)
            {
                callback(it, null)
                return@ensureValidAccessToken
            }
            
            val ahc = AsyncHttpClient()
            ahc.setUserAgent(USER_AGENT)
            ahc.addHeader("Authorization", "bearer " + accessToken!!)
    
            val params = RequestParams()
            params.add("api_type", "json")
            params.add("kind", if (post.type) "link" else "self")
            params.add("resubmit", if (resubmit) "true" else "false")
            params.add("sendreplies", if (sendReplies) "true" else "false")
            params.add("sr", post.subreddit)
            params.add(if (post.type) "url" else "text", post.content)
            params.add("title", post.title)
            
            ahc.post(SUBMIT_ENDPOINT, params, object : JsonHttpResponseHandler() {
                
                override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject?) {
                    super.onSuccess(statusCode, headers, response)
    
                    val json = response!!.optJSONObject("json")
                    val errors = json!!.optJSONArray("errors")
    
                    if (errors.length() == 0) {
                        lastSubmissionDate = Date()
                        callbacks.onNewLastSubmissionDate()
                        callback(null, json.optJSONObject("data").optString("url"))
                    } else {
                        val errorString = StringBuilder("You got ${errors.length()} errors: ")
                        for (i in 0 until errors.length()) {
                            val error = errors.optJSONArray(i)
                            val name = error.optString(0)
                            val description = error.optString(1)
    
                            when (name) {
                                "RATELIMIT", "NO_URL" -> errorString.append(name).append(" ").append(description).append("\n")
                                else -> {
                                    callback(RuntimeException("I FOUND AN UNKNOWN ERROR:\nNAME: $name\nDESCRIPTION: $description"), null)
                                    return
                                }
                            }
                        }
                        
                        callback(RuntimeException(errorString.toString()), null)
                    }
                }
    
                override fun onFailure(statusCode: Int, headers: Array<Header>?, throwable: Throwable, errorResponse: JSONObject?) {
                    super.onFailure(statusCode, headers, throwable, errorResponse)
                    
                    callback(throwable, null)
                }
    
                override fun onSuccess(statusCode: Int, headers: Array<Header>?, responseString: String?) {
                    super.onSuccess(statusCode, headers, responseString)
                    
                    callback(RuntimeException("RESPONSE STRING ONSUCCESS"), null)
                }
    
                override fun onFailure(statusCode: Int, headers: Array<Header>?, responseString: String?, throwable: Throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable)

                    callback(throwable, null)
                }
            })
        })
    }

    private fun ensureValidAccessToken(callback: (Throwable?) -> Unit) {
        if (accessToken != null && accessTokenExpirationDate!!.after(Date())) {
            callback(null)
            return
        }
        
        if (refreshToken == null)
        {
            callback(RuntimeException("Can't update access token without refresh token"))
            return
        }

        val ahc = AsyncHttpClient()
        ahc.setUserAgent(USER_AGENT)
        ahc.setBasicAuth(APP_CLIENT_ID, APP_CLIENT_SECRET)

        val params = RequestParams()
        params.add("grant_type", "refresh_token")
        params.add("refresh_token", refreshToken)
        ahc.post(ACCESS_TOKEN_ENDPOINT, params, object : JsonHttpResponseHandler() {
            
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject?) {
                super.onSuccess(statusCode, headers, response)

                try {
                    // TODO: compare received scope with intended scope?
                    updateAccessToken(response!!.getString("access_token"), response.getInt("expires_in"))
                } catch (e: Exception) {
                    callback(e)
                    return
                }

                callback(null)
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>?, throwable: Throwable, errorResponse: JSONObject?) {
                super.onFailure(statusCode, headers, throwable, errorResponse)
                
                callback(throwable)
                return
            }
        })
    }

    private fun updateAccessToken(newAccessToken: String, expiresIn: Int) {
        accessToken = newAccessToken
        val date = Calendar.getInstance() // current time
        date.add(Calendar.SECOND, expiresIn) // expiresIn == 1 hour
        accessTokenExpirationDate = date.time
        
        callbacks.onNewAccessToken()
    }

    fun fetchAuthTokens(callback: (Throwable?) -> Unit) {
        if (authCode == null)
        {
            callback(RuntimeException("Can't fetch auth tokens without auth code"))
            return
        }

        val ahc = AsyncHttpClient()
        ahc.setUserAgent(USER_AGENT)
        ahc.setBasicAuth(APP_CLIENT_ID, APP_CLIENT_SECRET)

        val params = RequestParams()
        params.add("grant_type", "authorization_code")
        params.add("code", authCode)
        params.add("redirect_uri", APP_REDIRECT_URI)
        ahc.post(ACCESS_TOKEN_ENDPOINT, params, object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>?, response: JSONObject?) {
                super.onSuccess(statusCode, headers, response)

                try {
                    // TODO: compare received scope with intended scope?
                    refreshToken = response!!.getString("refresh_token")
                    updateAccessToken(response.getString("access_token"), response.getInt("expires_in"))
                } catch (e: Exception) {
                    callback(e)
                    return
                }

                callbacks.onNewRefreshToken()
                callback(null)
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>?, throwable: Throwable, errorResponse: JSONObject?) {
                super.onFailure(statusCode, headers, throwable, errorResponse)

                callback(throwable)
            }
        })
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

    companion object {
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

        private const val RATELIMIT = 10 * 60 * 1000 // in milliseconds, also 10 minutes

        private fun randomState(): String { // TODO: implement this method
            return "testy"
        }
    }
}

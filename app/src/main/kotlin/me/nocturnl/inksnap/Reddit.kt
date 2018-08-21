package me.nocturnl.inksnap

import android.net.Uri
import khttp.structures.authorization.BasicAuthorization
import org.joda.time.DateTime
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.randomState
import java.net.HttpURLConnection

class Reddit private constructor(private val callbacks: Callbacks)
{
    private var authState: String? = null
    private var authCode: String? = null

    var accessToken: String? = null
    var refreshToken: String? = null
    var accessTokenExpirationDate: DateTime? = null
    var lastSubmissionDate: DateTime? = null
    var name: String? = null

    val isLoggedIn get() = refreshToken != null

    val authorizationUrl: String
        get()
        {
            authState = randomState()
            return Uri.parse(AUTORIZE_ENDPOINT).buildUpon()
                    .appendQueryParameter("client_id", APP_CLIENT_ID)
                    .appendQueryParameter("response_type", AUTH_RESPONSE_TYPE)
                    .appendQueryParameter("state", authState)
                    .appendQueryParameter("redirect_uri", APP_REDIRECT_URI)
                    .appendQueryParameter("duration", AUTH_DURATION)
                    .appendQueryParameter("scope", AUTH_SCOPE).build().toString()
        }

    interface Callbacks
    {
        fun onNewAccessToken() // todo: make these pass args like imgur's callbacks
        fun onNewRefreshToken()
        fun onNewLastSubmissionDate()
        fun onNewName()
    }
    
    fun logout()
    {
        // todo: properly logout by making a request to make the these tokens invalid
        
        accessToken = null
        refreshToken = null
        accessTokenExpirationDate = null
        lastSubmissionDate = null
        name = null
        
        callbacks.onNewAccessToken()
        callbacks.onNewRefreshToken()
        callbacks.onNewLastSubmissionDate()
        callbacks.onNewName()
    }

    suspend fun submit(post: Post,
               resubmit: Boolean = true,
               sendReplies: Boolean = true): String
    {
        if (!post.isValid(false))
        {
            throw Exception("Invalid post: ${post.reasonWhyInvalid(false)}")
        }

        ensureValidAccessToken()

        val headers =
                mapOf("User-Agent" to USER_AGENT,
                      "Authorization" to "bearer ${accessToken!!}")

        val data = mapOf(
                "api_type" to "json",
                "kind" to if (post.isLink) "link" else "self",
                "resubmit" to if (resubmit) "true" else "false",
                "sendreplies" to if (sendReplies) "true" else "false",
                "sr" to post.subreddit,
                (if (post.isLink) "url" else "text") to post.content,
                "title" to post.title)

        val response = Util.httpPostAsync(url = SUBMIT_ENDPOINT, headers = headers, data = data).await()

        if (response.statusCode != HttpURLConnection.HTTP_OK)
        {
            throw Exception("Response code: ${response.statusCode}, response: ${response.text}")
        }

        val json = response.jsonObject.getJSONObject("json")
        val errors = json.getJSONArray("errors")
        
        if (errors.length() == 0)
        {
            lastSubmissionDate = DateTime.now()
            callbacks.onNewLastSubmissionDate()
            return json.getJSONObject("data").getString("url") // post link
        }
        else
        {
            val error = errors.getJSONArray(0)
            val errorName = error.getString(0)
            val errorDescription = error.getString(1)
            
            val reasonTitle: String?
            val detailedReason: String?
            
            when (errorName)
            {
                "RATELIMIT" ->
                {
                    reasonTitle = "Ratelimit"
                    detailedReason = "Reddit does not allow posting too often.\n\nReddit says:\n$errorDescription"
                }
                
                "NO_URL" ->
                {
                    reasonTitle = "No url"
                    detailedReason = "Reddit says:\n$errorDescription"
                }
                
                else ->
                {
                    reasonTitle = errorName
                    detailedReason = "Reddit says:\n$errorDescription"
                }
            }
            
            throw SubmissionException(reasonTitle!!, detailedReason)
        }
    }

    suspend fun fetchName()
    {
        ensureValidAccessToken()

        val headers =
                mapOf("User-Agent" to USER_AGENT,
                      "Authorization" to "bearer ${accessToken!!}")
        
        val response = Util.httpGetAsync(url = NAME_ENDPOINT, headers = headers).await()

        if (response.statusCode != HttpURLConnection.HTTP_OK)
        {
            throw Exception("Response code: ${response.statusCode}, response: ${response.text}")
        }
        
        val json = response.jsonObject
        
        name = json.getString("name")
        callbacks.onNewName()
    }

    private suspend fun ensureValidAccessToken()
    {
        if (accessToken != null && accessTokenExpirationDate!! > DateTime.now())
        {
            return
        }

        val refreshToken =
                refreshToken ?: throw Exception("Can't update access token without refresh token")

        val headers = mapOf("User-Agent" to USER_AGENT)

        val data = mapOf("grant_type" to "refresh_token", "refresh_token" to refreshToken)

        val auth = BasicAuthorization(APP_CLIENT_ID, APP_CLIENT_SECRET)

        val response = Util.httpPostAsync(url = ACCESS_TOKEN_ENDPOINT, headers = headers, data = data, auth = auth).await()

        if (response.statusCode != HttpURLConnection.HTTP_OK)
        {
            throw Exception("Response code: ${response.statusCode}, response: ${response.text}")
        }

        val json = response.jsonObject

        val accessToken = json.getString("access_token")
        val expiresIn = json.getInt("expires_in")

        updateAccessToken(accessToken, expiresIn)
    }

    private fun updateAccessToken(newAccessToken: String, expiresIn: Int)
    {
        accessToken = newAccessToken
        accessTokenExpirationDate = DateTime.now() + Duration.standardSeconds(expiresIn.toLong())

        callbacks.onNewAccessToken()
    }

    suspend fun fetchAuthTokens()
    {
        val authCode = authCode ?: throw Exception("Can't fetch auth tokens without auth code")

        val headers = mapOf("User-Agent" to USER_AGENT)

        val data = mapOf("grant_type" to "authorization_code",
                         "code" to authCode,
                         "redirect_uri" to APP_REDIRECT_URI)

        val auth = BasicAuthorization(APP_CLIENT_ID, APP_CLIENT_SECRET)

        val response = Util.httpPostAsync(url = ACCESS_TOKEN_ENDPOINT, headers = headers, data = data, auth = auth).await()

        if (response.statusCode != HttpURLConnection.HTTP_OK)
        {
            throw Exception("Response code: ${response.statusCode}, response: ${response.text}")
        }

        val json = response.jsonObject

        // TODO: compare received scope with intended scope?
        refreshToken = json.getString("refresh_token")
        val accessToken = json.getString("access_token")
        val expiresIn = json.getInt("expires_in")

        updateAccessToken(accessToken, expiresIn)

        callbacks.onNewRefreshToken()
    }

    fun tryExtractCode(url: String): Boolean
    {
        if (!url.startsWith(APP_REDIRECT_URI) || authState == null)
        {
            return false
        }

        val uri = Uri.parse(url)
        val state = uri.getQueryParameter("state")
        if (state == null || state != authState)
        {
            return false
        }

        authState = null
        authCode = uri.getQueryParameter("code")
        return authCode != null // 'true' denotes success
    }

    companion object : SingletonHolder<Reddit, Callbacks>(::Reddit)
    {
        private const val APP_CLIENT_ID = VariantVariables.REDDIT_CLIENT_ID
        private const val APP_CLIENT_SECRET = VariantVariables.REDDIT_CLIENT_SECRET
        private const val APP_REDIRECT_URI = "http://localhost"

        private const val AUTORIZE_ENDPOINT = "https://www.reddit.com/api/v1/authorize.compact"
        private const val ACCESS_TOKEN_ENDPOINT = "https://www.reddit.com/api/v1/access_token"
        private const val SUBMIT_ENDPOINT = "https://oauth.reddit.com/api/submit"
        private const val NAME_ENDPOINT = "https://oauth.reddit.com/api/v1/me"

        private const val AUTH_DURATION = "permanent"
        private const val AUTH_SCOPE = "identity submit"
        private const val AUTH_RESPONSE_TYPE = "code"
        private const val USER_AGENT = Util.USER_AGENT
        
        const val POST_TITLE_LENGTH_LIMIT = 300
        const val POST_TEXT_LENGTH_LIMIT = 40000
        const val SUBREDDIT_NAME_LENGTH_LIMIT = 21
    }
}

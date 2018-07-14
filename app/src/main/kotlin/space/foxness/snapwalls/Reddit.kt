package space.foxness.snapwalls

import android.net.Uri
import khttp.structures.authorization.BasicAuthorization
import org.joda.time.DateTime
import org.joda.time.Duration
import space.foxness.snapwalls.Util.randomState

class Reddit private constructor(private val callbacks: Callbacks)
{

    private var authState: String? = null
    private var authCode: String? = null

    var accessToken: String? = null
    var refreshToken: String? = null
    var accessTokenExpirationDate: DateTime? = null
    var lastSubmissionDate: DateTime? = null

    val isLoggedIn get() = refreshToken != null

    val isRestrictedByRatelimit
        get() = lastSubmissionDate != null && DateTime.now() < lastSubmissionDate!! + Duration(
                RATELIMIT_MS)

    val canSubmitRightNow get() = isLoggedIn && !isRestrictedByRatelimit

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
        fun onNewAccessToken()
        fun onNewRefreshToken()
        fun onNewLastSubmissionDate()
    }

    fun submit(post: Post,
               debugDontPost: Boolean = false,
               resubmit: Boolean = true,
               sendReplies: Boolean = true): String
    {
        // todo: use postfragment's definition of bad post
        if (post.title.isEmpty() || (post.isLink && post.content.isEmpty()) || post.subreddit.isEmpty())
        {
            throw Exception("Bad post")
        }

        if (debugDontPost)
        {
            return "DEBUG: POST NOT SUBMITTED"
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

        val response = khttp.post(url = SUBMIT_ENDPOINT, headers = headers, data = data)

        if (response.statusCode != 200)
        {
            throw Exception("Response code: ${response.statusCode}, response: $response")
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
            var errorString = "You got ${errors.length()} errors: "
            for (i in 0 until errors.length())
            {
                val error = errors.getJSONArray(i)
                val name = error.getString(0)
                val description = error.getString(1)

                val currentError = when (name)
                {
                    "RATELIMIT", "NO_URL" -> "[NAME]: $name [DESCRIPTION]: $description"
                    else -> "[NAME OF AN UNKNOWN ERROR]: $name [DESCRIPTION OF AN UNKNOWN ERROR]: $description"
                }

                errorString += "$currentError\n"
            }

            throw Exception(errorString)
        }
    }

    private fun ensureValidAccessToken()
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

        val response =
                khttp.post(url = ACCESS_TOKEN_ENDPOINT, headers = headers, data = data, auth = auth)

        if (response.statusCode != 200)
        {
            throw Exception("Response code: ${response.statusCode}, response: $response")
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

    fun fetchAuthTokens()
    {
        val authCode = authCode ?: throw Exception("Can't fetch auth tokens without auth code")

        val headers = mapOf("User-Agent" to USER_AGENT)

        val data = mapOf("grant_type" to "authorization_code",
                         "code" to authCode,
                         "redirect_uri" to APP_REDIRECT_URI)

        val auth = BasicAuthorization(APP_CLIENT_ID, APP_CLIENT_SECRET)

        val response =
                khttp.post(url = ACCESS_TOKEN_ENDPOINT, headers = headers, data = data, auth = auth)

        if (response.statusCode != 200)
        {
            throw Exception("Response code: ${response.statusCode}, response: $response")
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

        private const val AUTH_DURATION = "permanent"
        private const val AUTH_SCOPE = "submit"
        private const val AUTH_RESPONSE_TYPE = "code"
        private const val USER_AGENT = Util.USER_AGENT

        private const val RATELIMIT_MS: Long = 10 * 60 * 1000 // 10 minutes, required to be long
    }
}

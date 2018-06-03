package space.foxness.snapwalls

import android.net.Uri
import org.joda.time.DateTime
import org.joda.time.Duration

class ImgurAccount(private val callbacks: Callbacks) {

    interface Callbacks {
        fun onNewAccessToken(newAccessToken: String, newAccessTokenExpirationDate: DateTime)
        fun onNewRefreshToken(newRefreshToken: String)
    }

    private var authState: String? = null

    var accessToken: String? = null
    var refreshToken: String? = null
    var accessTokenExpirationDate: DateTime? = null
    
    val isLoggedIn get() = refreshToken != null
    
    val authorizationUrl: String
        get() {
            authState = randomState()
            return Uri.parse(AUTHORIZATION_ENDPOINT).buildUpon()
                    .appendQueryParameter("client_id", APP_CLIENT_ID)
                    .appendQueryParameter("response_type", AUTH_RESPONSE_TYPE)
                    .appendQueryParameter("state", authState)
                    .build().toString()
        }
    
    fun tryExtractTokens(url: String): Boolean {
        if (!url.startsWith(APP_REDIRECT_URI) || authState == null)
            return false
        
        val uri = Uri.parse(url.replace('#', '&'))
        val state = uri.getQueryParameter("state")
        if (state == null || state != authState)
            return false
        
        authState = null
        
        val newAccessToken = uri.getQueryParameter("access_token")!!
        val newRefreshToken = uri.getQueryParameter("refresh_token")!!
        val expiresIn = uri.getQueryParameter("expires_in")!!.toLong()

        updateRefreshToken(newRefreshToken)
        updateAccessToken(newAccessToken, expiresIn)
        
        return true
    }
    
    private fun updateRefreshToken(newRefreshToken: String) {
        refreshToken = newRefreshToken
        callbacks.onNewRefreshToken(refreshToken!!)
    }

    private fun updateAccessToken(newAccessToken: String, expiresIn: Long) {
        accessToken = newAccessToken
        accessTokenExpirationDate = DateTime.now() + Duration.standardSeconds(expiresIn)

        callbacks.onNewAccessToken(accessToken!!, accessTokenExpirationDate!!)
    }
    
    companion object {
        private const val APP_CLIENT_ID = "f74e1f0ca375c60"
        private const val APP_CLIENT_SECRET = "9d7910e7976b44b36feaafdd7e0cd3e74daad29a"
        private const val APP_REDIRECT_URI = "https://localhost/"
        
        private const val AUTH_RESPONSE_TYPE = "token"

        private const val IMAGE_UPLOAD_ENDPOINT = "https://api.imgur.com/3/image"
        private const val AUTHORIZATION_ENDPOINT = "https://api.imgur.com/oauth2/authorize"

        private const val USER_AGENT = "Snapwalls by /u/foxneZz" // todo: extract this to util?
        
        fun upload(url: String): String {
            val headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Authorization" to "Client-ID $APP_CLIENT_ID")

            val data = mapOf(
                    "image" to url,
                    "type" to "URL")

            val response = khttp.post(url = IMAGE_UPLOAD_ENDPOINT, headers = headers, data = data)

            if (response.statusCode != 200)
                throw Exception("Response code: ${response.statusCode}, response: $response")

            val json = response.jsonObject

            val link = json.getJSONObject("data").getString("link")
            return link
        }

        private fun randomState(): String { // TODO: implement this method
            return "testy"
        }
    }
}
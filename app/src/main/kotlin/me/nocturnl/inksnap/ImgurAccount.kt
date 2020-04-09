package me.nocturnl.inksnap

import android.net.Uri
import android.webkit.URLUtil.isValidUrl
import org.joda.time.DateTime
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.randomState
import org.json.JSONObject
import java.net.HttpURLConnection

class ImgurAccount(private val callbacks: Callbacks)
{
    interface Callbacks
    {
        fun onNewAccessToken(newAccessToken: String?, newAccessTokenExpirationDate: DateTime?)
        fun onNewRefreshToken(newRefreshToken: String?)
    }

    private var authState: String? = null

    var accessToken: String? = null
    var refreshToken: String? = null
    var accessTokenExpirationDate: DateTime? = null

    val isLoggedIn get() = refreshToken != null

    val authorizationUrl: String
        get()
        {
            authState = randomState()
            return Uri.parse(AUTHORIZATION_ENDPOINT).buildUpon()
                    .appendQueryParameter("client_id", APP_CLIENT_ID)
                    .appendQueryParameter("response_type", AUTH_RESPONSE_TYPE)
                    .appendQueryParameter("state", authState).build().toString()
        }
    
    fun logout()
    {
        // todo: properly logout by making a request to make the these tokens invalid
        
        accessToken = null
        refreshToken = null
        accessTokenExpirationDate = null
        
        callbacks.onNewAccessToken(accessToken, accessTokenExpirationDate)
        callbacks.onNewRefreshToken(refreshToken)
    }

    fun tryExtractTokens(url: String): Boolean
    {
        if (!url.startsWith(APP_REDIRECT_URI) || authState == null)
        {
            return false
        }

        val uri = Uri.parse(url.replace('#', '&'))
        val state = uri.getQueryParameter("state")
        if (state == null || state != authState)
        {
            return false
        }

        authState = null

        val newAccessToken = uri.getQueryParameter("access_token")!!
        val newRefreshToken = uri.getQueryParameter("refresh_token")!!
        val expiresIn = uri.getQueryParameter("expires_in")!!.toLong()

        updateRefreshToken(newRefreshToken)
        updateAccessToken(newAccessToken, expiresIn)

        return true
    }

    private fun updateRefreshToken(newRefreshToken: String)
    {
        refreshToken = newRefreshToken
        callbacks.onNewRefreshToken(refreshToken!!)
    }

    private fun updateAccessToken(newAccessToken: String, expiresIn: Long)
    {
        accessToken = newAccessToken
        accessTokenExpirationDate = DateTime.now() + Duration.standardSeconds(expiresIn)

        callbacks.onNewAccessToken(accessToken!!, accessTokenExpirationDate!!)
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

        val data = mapOf("grant_type" to "refresh_token",
                         "refresh_token" to refreshToken,
                         "client_id" to APP_CLIENT_ID,
                         "client_secret" to APP_CLIENT_SECRET)

        val response = Util.httpPostAsync(url = ACCESS_TOKEN_ENDPOINT, headers = headers, data = data).await()

        if (response.code != HttpURLConnection.HTTP_OK)
        {
            throw Exception("Response code: ${response.code}, response: ${response.body}")
        }

        val json = JSONObject(response.body.toString())

        val newAccessToken = json.getString("access_token")
        val expiresIn = json.getLong("expires_in")

        // idk why imgur returns a new refresh token here
        // the old refresh token keeps working but i save the new one just in case
        val newRefreshToken = json.getString("refresh_token")

        updateRefreshToken(newRefreshToken)
        updateAccessToken(newAccessToken, expiresIn)
    }
    
    data class ImgurImage(val link: String, val width: Int, val height: Int)

    suspend fun uploadImage(url: String): ImgurImage
    {
        if (!isValidUrl(url))
        {
            throw Exception("Invalid url: $url")
        }

        ensureValidAccessToken()

        val headers =
                mapOf("User-Agent" to USER_AGENT, 
                      "Authorization" to "Bearer ${accessToken!!}")

        val data = mapOf("image" to url, "type" to "URL")

        val response = Util.httpPostAsync(url = IMAGE_UPLOAD_ENDPOINT, headers = headers, data = data).await()

        if (response.code != HttpURLConnection.HTTP_OK)
        {
            throw Exception("Response code: ${response.code}, response: ${response.body}")
        }

        val json = JSONObject(response.body.toString())
        if (!json.getBoolean("success"))
        {
            throw Exception("JSON response success: false; JSON: $json")
        }
        
        val jsonData = json.getJSONObject("data")

        val link = jsonData.getString("link")
        val width = jsonData.getInt("width")
        val height = jsonData.getInt("height")
        
        return ImgurImage(link, width, height)
    }

    companion object
    {
        private const val APP_CLIENT_ID = VariantVariables.IMGUR_CLIENT_ID
        private const val APP_CLIENT_SECRET = VariantVariables.IMGUR_CLIENT_SECRET
        private const val APP_REDIRECT_URI = "https://localhost/"

        private const val AUTH_RESPONSE_TYPE = "token"

        private const val IMAGE_UPLOAD_ENDPOINT = "https://api.imgur.com/3/image"
        private const val AUTHORIZATION_ENDPOINT = "https://api.imgur.com/oauth2/authorize"
        private const val ACCESS_TOKEN_ENDPOINT = "https://api.imgur.com/oauth2/token"

        private const val USER_AGENT = Util.USER_AGENT

//        fun uploadAnonymously(url: String): String // warning: heavily compresses the image
//        {
//            val headers =
//                    mapOf("User-Agent" to USER_AGENT, "Authorization" to "Client-ID $APP_CLIENT_ID")
//
//            val data = mapOf("image" to url, "type" to "URL")
//
//            val response = khttp.post(url = IMAGE_UPLOAD_ENDPOINT, headers = headers, data = data)
//
//            if (response.statusCode != 200)
//            {
//                throw Exception("Response code: ${response.statusCode}, response: $response")
//            }
//
//            val json = response.jsonObject
//            if (!json.getBoolean("success"))
//            {
//                throw Exception("JSON response success: false; JSON: $json")
//            }
//
//            val link = json.getJSONObject("data").getString("link")
//            return link
//        }
    }
}
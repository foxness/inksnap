package space.foxness.snapwalls

class Imgur {
    
    companion object {
        private const val APP_CLIENT_ID = "f74e1f0ca375c60"
        private const val APP_CLIENT_SECRET = "9d7910e7976b44b36feaafdd7e0cd3e74daad29a"

        private const val IMAGE_UPLOAD_ENDPOINT = "https://api.imgur.com/3/image"

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
    }
}
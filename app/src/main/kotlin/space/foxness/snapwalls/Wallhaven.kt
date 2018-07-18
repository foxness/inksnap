package space.foxness.snapwalls

object Wallhaven
{
    private val wallhavenRegex = """https://alpha\.wallhaven\.cc/wallpaper/(?<id>\d+)/?""".toRegex()
    
    fun tryGetDirectUrl(url: String): String?
    {
        if (wallhavenRegex.matches(url))
        {
            val headers = mapOf("User-Agent" to Util.USER_AGENT)
            val response = khttp.get(url = url, headers = headers)

            if (response.statusCode != 200)
            {
                return null
            }

            val rawHtml = response.text

            // <img id="wallpaper" src="//wallpapers.wallhaven.cc/wallpapers/full/wallhaven-599344.jpg"
            val startTag = "<img id=\"wallpaper\" src=\""
            var startIndex = rawHtml.indexOf(startTag)

            if (startIndex == -1)
            {
                return null
            }

            startIndex += startTag.length

            val endIndex = rawHtml.indexOf('"', startIndex)

            if (endIndex == -1)
            {
                return null
            }

            val extractedUrl = rawHtml.substring(startIndex, endIndex)
            return "https:$extractedUrl"
        }
        else
        {
            return null
        }
    }
    
    fun tryGetThumbnailUrl(url: String): String?
    {
        val id = tryGetWallpaperId(url) ?: return null
        return "https://wallpapers.wallhaven.cc/wallpapers/thumb/small/th-$id.jpg"
    }
    
    private fun tryGetWallpaperId(url: String): String?
    {
        return wallhavenRegex.matchEntire(url)?.groups?.get("id")?.value
    }
}
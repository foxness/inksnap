package space.foxness.snapwalls

object UrlProcessor
{
    private val wallhavenRegex = """https://alpha\.wallhaven\.cc/wallpaper/\d+/?""".toRegex()
    
    fun process(url: String): String
    {
        if (wallhavenRegex.matches(url))
        {
            val userAgent = "Snapwalls by /u/foxneZz" // todo: refactor all useragents into util or smth
            val headers = mapOf("User-Agent" to userAgent)
            val response = khttp.get(url = url, headers = headers)
            
            if (response.statusCode != 200)
            {
                return url
            }
            
            val rawHtml = response.text
            
            // <img id="wallpaper" src="//wallpapers.wallhaven.cc/wallpapers/full/wallhaven-599344.jpg"
            val startTag = "<img id=\"wallpaper\" src=\""
            var startIndex = rawHtml.indexOf(startTag)
            
            if (startIndex == -1)
            {
                return url
            }
            
            startIndex += startTag.length
            
            val endIndex = rawHtml.indexOf('"', startIndex)
            
            if (endIndex == -1)
            {
                return url
            }
            
            val extractedUrl = rawHtml.substring(startIndex, endIndex)
            
            return "https:$extractedUrl"
        }
        else
        {
            return url
        }
    }
}
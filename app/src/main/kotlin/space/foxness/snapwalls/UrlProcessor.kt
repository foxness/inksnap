package space.foxness.snapwalls

object UrlProcessor
{
    fun process(url: String): String
    {
        return Wallhaven.tryGetDirectUrl(url)
    }
}
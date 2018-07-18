package space.foxness.snapwalls

object ServiceProcessor // todo: add support for services other than wallhaven?
{
    fun tryGetDirectUrl(url: String): String?
    {
        return Wallhaven.tryGetDirectUrl(url)
    }
    
    fun tryGetThumbnailUrl(url: String): String?
    {
        return Wallhaven.tryGetThumbnailUrl(url)
    }
}
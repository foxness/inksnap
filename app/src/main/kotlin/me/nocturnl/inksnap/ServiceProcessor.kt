package me.nocturnl.inksnap

import me.nocturnl.inksnap.Util.isImageUrl

object ServiceProcessor // todo: add support for services other than wallhaven?
{
    suspend fun tryGetDirectUrl(url: String): String?
    {
        return Wallhaven.tryGetDirectUrl(url) ?: if (url.isImageUrl())
        {
            url
        }
        else
        {
            null
        }
    }
    
    fun tryGetThumbnailUrl(url: String): String?
    {
        return Wallhaven.tryGetThumbnailUrl(url)
    }
    
    suspend fun tryGetThumbnailOrDirectUrl(url: String): String?
    {
        return tryGetThumbnailUrl(url) ?: tryGetDirectUrl(url)
    }
}
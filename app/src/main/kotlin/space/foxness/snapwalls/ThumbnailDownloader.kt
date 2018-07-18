package space.foxness.snapwalls

import android.os.HandlerThread
import space.foxness.snapwalls.Util.log

class ThumbnailDownloader<T> : HandlerThread(TAG)
{
    fun queueThumbnail(target: T, url: String)
    {
        log("Got a URL: $url")
    }
    
    companion object
    {
        private const val TAG = "ThumbnailDownloader"
    }
}
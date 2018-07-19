package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import java.util.concurrent.ConcurrentHashMap

class ThumbnailDownloader<T>(private val responseHandler: Handler) : HandlerThread(TAG)
{
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()

    private lateinit var thumbnailDownloadListener: ThumbnailDownloadListener<T>

    interface ThumbnailDownloadListener<T>
    {
        fun onThumbnailDownloaded(target: T, thumbnail: Bitmap)
    }
    
    fun setThumbnailDownloadListener(listener: ThumbnailDownloadListener<T>)
    {
        thumbnailDownloadListener = listener
    }

    override fun onLooperPrepared()
    {
        requestHandler = @SuppressLint("HandlerLeak")
        object : Handler()
        {
            override fun handleMessage(msg: Message)
            {
                if (msg.what == MESSAGE_DOWNLOAD)
                {
                    @Suppress("UNCHECKED_CAST")
                    val target = msg.obj as T
                    handleRequest(target)
                }
            }
        }
    }
    
    fun queueThumbnail(target: T, url: String)
    {
        requestMap[target] = url
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()
    }
    
    fun unqueueThumbnail(target: T)
    {
        requestMap.remove(target)
    }
    
    fun clearQueue()
    {
        requestHandler.removeMessages(MESSAGE_DOWNLOAD)
    }
    
    private fun handleRequest(target: T)
    {
        val url = requestMap[target] ?: return

        val thumbnailBytes = Util.downloadBytesFromUrl(url)!!
        val thumbnailBitmap = Util.getBitmapFromBytes(thumbnailBytes)!!

        responseHandler.post {
            if (requestMap[target] != url)
            {
                return@post
            }

            requestMap.remove(target)
            thumbnailDownloadListener.onThumbnailDownloaded(target, thumbnailBitmap)
        }
    }
    
    companion object
    {
        private const val TAG = "ThumbnailDownloader"
        private const val MESSAGE_DOWNLOAD = 0
    }
}
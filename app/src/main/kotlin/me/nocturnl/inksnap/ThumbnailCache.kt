package me.nocturnl.inksnap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ThumbnailCache private constructor(context_: Context)
{
    private val context: Context = context_.applicationContext
    
    init
    {
        val thumbnailDirectory = getThumbnailDirectory()
        if (!thumbnailDirectory.exists())
        {
            thumbnailDirectory.mkdir()
        }
    }
    
    fun add(thumbnailId: String, thumbnail: Bitmap)
    {
        val file = getThumbnailFile(thumbnailId)
        
        if (file.exists())
        {
            throw Exception("Thumbnail already in cache")
        }
        
        file.createNewFile()
        val outStream = FileOutputStream(file)
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        outStream.close()
    }
    
    fun contains(thumbnailId: String) = getThumbnailFile(thumbnailId).exists()
    
    fun get(thumbnailId: String): Bitmap?
    {
        val file = getThumbnailFile(thumbnailId)
        
        if (!file.exists())
        {
            return null
        }
        
        val inStream = FileInputStream(file)
        val thumbnail = BitmapFactory.decodeStream(inStream)!!
        
        return thumbnail
    }
    
    fun remove(thumbnailId: String)
    {
        val file = getThumbnailFile(thumbnailId)
        if (file.exists())
        {
            file.delete()
        }
        else
        {
            throw Exception("Thumbnail doesn't exist")
        }
    }
    
    private fun getThumbnailFile(thumbnailId: String): File
    {
        return File(context.cacheDir.absolutePath
                    + File.separator
                    + DIRECTORY_NAME
                    + File.separator
                    + thumbnailId
                    + THUMBNAIL_EXTENSION)
    }

    private fun getThumbnailDirectory(): File
    {
        return File(context.cacheDir.absolutePath
                    + File.separator
                    + DIRECTORY_NAME
                    + File.separator)
    }
    
    companion object : SingletonHolder<ThumbnailCache, Context>(::ThumbnailCache)
    {
        private const val DIRECTORY_NAME = "thumbnails"
        private const val THUMBNAIL_EXTENSION = ".jpg"
    }
}
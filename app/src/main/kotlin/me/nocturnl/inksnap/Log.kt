package me.nocturnl.inksnap

import android.content.Context
import org.joda.time.DateTime
import java.io.File

class Log private constructor(context_: Context)
{
    private val context: Context = context_.applicationContext
    
    fun log(message: String)
    {
        val entry = "[${DateTime.now()}] $message\n"
        context.openFileOutput(FILE_NAME, Context.MODE_APPEND).write(entry.toByteArray())
        // todo: log to debug console too
    }
    
    fun clear()
    {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).write("".toByteArray())
    }
    
//    fun delete()
//    {
//        context.deleteFile(FILE_NAME)
//    }
    
    private fun exists() = File(context.filesDir, FILE_NAME).exists()
    
    fun get(): String
    {
        return if (exists())
        {
            context.openFileInput(FILE_NAME).bufferedReader().readText()
        }
        else
        {
            ""
        }
    }
    
    companion object : SingletonHolder<Log, Context>(::Log)
    {
        private const val FILE_NAME = "log"
    }
}
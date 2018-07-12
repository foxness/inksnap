package space.foxness.snapwalls

import android.content.Context
import org.joda.time.DateTime
import java.io.File

object Log
{
    private const val FILE_NAME = "log"
    
    fun log(context: Context, message: String)
    {
        val entry = "[${DateTime.now()}] $message\n"
        context.openFileOutput(FILE_NAME, Context.MODE_APPEND).write(entry.toByteArray())
        // todo: log to debug console too
    }
    
    fun clear(context: Context)
    {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).write("".toByteArray())
    }
    
//    fun delete(context: Context)
//    {
//        context.deleteFile(FILE_NAME)
//    }
    
    private fun exists(context: Context) = File(context.filesDir, FILE_NAME).exists()
    
    fun get(context: Context): String
    {
        return if (exists(context))
        {
            context.openFileInput(FILE_NAME).bufferedReader().readText()
        }
        else
        {
            ""
        }
    } 
}
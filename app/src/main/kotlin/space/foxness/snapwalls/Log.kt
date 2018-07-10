package space.foxness.snapwalls

import android.content.Context
import org.joda.time.DateTime

object Log
{
    private const val FILE_NAME = "log"
    
    fun log(context: Context, message: String)
    {
        val entry = "[${DateTime.now()}] $message\n"
        context.openFileOutput(FILE_NAME, Context.MODE_APPEND).write(entry.toByteArray())
    }
    
    fun get(context: Context) = context.openFileInput(FILE_NAME).bufferedReader().readText()
}
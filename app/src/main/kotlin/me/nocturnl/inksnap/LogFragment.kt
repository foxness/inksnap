package me.nocturnl.inksnap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import me.nocturnl.inksnap.Util.toast

class LogFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_log, container, false)
        val log = Log.getInstance(context!!)
        
        val logView = v.findViewById<TextView>(R.id.log_textview)
        logView.text = log.get()
        
        val clearButton = v.findViewById<Button>(R.id.log_clear)
        clearButton.setOnClickListener { 
            log.clear()
            logView.text = ""
        }
        
        val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val logCopyButton = v.findViewById<Button>(R.id.log_copy)
        logCopyButton.setOnClickListener {
            clipboard.primaryClip = ClipData.newPlainText("Log", logView.text)
            toast("Copied")
        }
        
        return v
    }
    
    companion object
    {
        fun newInstance() = LogFragment()
    }
}
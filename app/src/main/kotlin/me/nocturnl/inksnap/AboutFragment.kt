package me.nocturnl.inksnap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import me.nocturnl.inksnap.Util.toast

class AboutFragment : Fragment()
{
    // todo: fix all instances of SuppressLint
    @SuppressLint("SetTextI18n") // todo: fix
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_about, container, false)
        
        val appIconView = v.findViewById<ImageView>(R.id.app_icon)
        
        val settingsManager = SettingsManager.getInstance(context!!)
        
        var timesTapped = 0
        
        appIconView.setOnClickListener {
            when (++timesTapped)
            {
                1, DEVELOPER_TAP_COUNT + 1 -> settingsManager.developerOptionsUnlocked = false
                DEVELOPER_TAP_COUNT -> settingsManager.developerOptionsUnlocked = true
            }
        }
        
        val appVersionView = v.findViewById<TextView>(R.id.app_version_view)
        appVersionView.text = "version ${BuildConfig.VERSION_NAME}"
        
        val feedbackAndSupport = v.findViewById<LinearLayout>(R.id.feedback_and_support)
        feedbackAndSupport.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        val devContact = v.findViewById<LinearLayout>(R.id.contact_the_dev)
        devContact.setOnClickListener {
            val developerEmail = resources.getString(R.string.developer_email)
            
            val i = Intent(Intent.ACTION_SENDTO)
            i.data = Uri.parse("mailto:$developerEmail")
            
            // for some reason the result is a new task even if this is not present
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            startActivity(i)
        }
        
        return v
    }
    
    companion object
    {
        fun newInstance() = AboutFragment()
        
        private const val DEVELOPER_TAP_COUNT = 23
        
        private const val SUPPORT_URL = "https://reddit.com/r/inksnap" // todo: extract
    }
}
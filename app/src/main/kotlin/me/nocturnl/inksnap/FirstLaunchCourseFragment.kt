package me.nocturnl.inksnap

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class FirstLaunchCourseFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_first_launch_course, container, false)
        
        val loginButton = v.findViewById<Button>(R.id.launcher_login_button)
        loginButton.setOnClickListener {
            val ctx = context!!
            val settingsManager = SettingsManager.getInstance(ctx)
            settingsManager.firstLaunchCourseCompleted = true
            
            val i = MainActivity.newIntent(ctx)
            startActivity(i)
            activity!!.finish()
        }
        
        return v
    }
    
    companion object
    {
        fun newInstance() = FirstLaunchCourseFragment()
    }
}
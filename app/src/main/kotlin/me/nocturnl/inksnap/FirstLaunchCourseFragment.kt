package me.nocturnl.inksnap

import android.app.Activity
import android.content.Intent
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
            val i = RedditAuthActivity.newIntent(context!!)
            startActivityForResult(i, REQUEST_CODE_REDDIT_AUTH)
        }
        
        return v
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when (requestCode)
        {
            REQUEST_CODE_REDDIT_AUTH ->
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    val ctx = context!!
                    val settingsManager = SettingsManager.getInstance(ctx)
                    settingsManager.firstLaunchCourseCompleted = true
                    
                    val i = MainActivity.newIntent(ctx)
                    startActivity(i)
                    activity!!.finish()
                }
            }
            
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
    
    companion object
    {
        private const val REQUEST_CODE_REDDIT_AUTH = 0
        
        fun newInstance() = FirstLaunchCourseFragment()
    }
}
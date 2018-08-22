package me.nocturnl.inksnap

import android.content.Context
import android.content.Intent

class FirstLaunchCourseActivity : SingleFragmentActivity()
{
    override fun createFragment() = FirstLaunchCourseFragment.newInstance()
    
    companion object
    {
        fun newIntent(context: Context) = Intent(context, FirstLaunchCourseActivity::class.java)
    }
}
package me.nocturnl.inksnap

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class FirstLaunchCourseFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_first_launch_course, container, false)
        return v
    }
    
    companion object
    {
        fun newInstance() = FirstLaunchCourseFragment()
    }
}
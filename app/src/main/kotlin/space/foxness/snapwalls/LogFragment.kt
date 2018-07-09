package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class LogFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_log, container, false)
        return v
    }
    
    companion object
    {
        fun newInstance() = LogFragment()
    }
}
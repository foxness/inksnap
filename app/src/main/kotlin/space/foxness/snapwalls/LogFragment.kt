package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class LogFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        val ctx = context!!
        
        Log.log(ctx, "I am in onCreateView()")
        
        val v = inflater.inflate(R.layout.fragment_log, container, false)
        
        val log = v.findViewById<TextView>(R.id.log_textview)
        log.text = Log.get(ctx)
        
        return v
    }
    
    companion object
    {
        fun newInstance() = LogFragment()
    }
}
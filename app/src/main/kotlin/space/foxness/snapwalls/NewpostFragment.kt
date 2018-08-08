package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class NewpostFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_newpost, container, false)
        
        val viewPager = v.findViewById<ViewPager>(R.id.newpost_viewpager)
        viewPager.adapter = object : FragmentPagerAdapter(childFragmentManager)
        {
            override fun getItem(position: Int): Fragment
            {
                return SelfpostFragment.newInstance()
            }

            override fun getCount() = 2
        }
        
        return v
    }
    
    companion object
    {
        fun newInstance() = NewpostFragment()
    }
}
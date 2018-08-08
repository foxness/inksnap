package space.foxness.snapwalls

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import space.foxness.snapwalls.Util.log

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

            override fun getPageTitle(position: Int): CharSequence?
            {
                return when (position)
                {
                    0 -> "self"
                    1 -> "link"
                    else -> throw Exception("how")
                }
            }
        }
        
        val otsl = object : TabLayout.OnTabSelectedListener
        {
            override fun onTabSelected(tab: TabLayout.Tab)
            {
                log("tab ${tab.text} at ${tab.position} selected")
            }
            
            override fun onTabReselected(tab: TabLayout.Tab)
            {
                log("tab ${tab.text} at ${tab.position} reselected")
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab)
            {
                log("tab ${tab.text} at ${tab.position} unselected")
            }
        }
        
        val tabLayout = v.findViewById<TabLayout>(R.id.newpost_tablayout)
        tabLayout.addOnTabSelectedListener(otsl)
        
        return v
    }
    
    companion object
    {
        fun newInstance() = NewpostFragment()
    }
}
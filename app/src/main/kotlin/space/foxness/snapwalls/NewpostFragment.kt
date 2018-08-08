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
    private lateinit var post: Post
    private var newPost = false
    private var allowIntendedSubmitDateEditing = false
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val args = arguments!!
        if (args.getBoolean(ARG_NEW_POST))
        {
            newPost = true
            post = Post.newInstance()
            post.title = "testy" // todo: remove on production
            post.content = "besty" // same ^
            post.subreddit = "test" // same ^
        }
        else
        {
            post = args.getSerializable(ARG_POST) as Post
        }

        allowIntendedSubmitDateEditing = args.getBoolean(ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING)
    }
    
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
                return when (position)
                {
                    0 -> SelfpostFragment.newInstance(post, allowIntendedSubmitDateEditing)
                    1 -> LinkpostFragment.newInstance(post, allowIntendedSubmitDateEditing)
                    else -> throw Exception("how!@#")
                }
            }

            override fun getCount() = 2

            override fun getPageTitle(position: Int): CharSequence?
            {
                return when (position)
                {
                    0 -> "self" // todo: extract
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
        private const val ARG_POST = "post"
        private const val ARG_NEW_POST = "new_post"
        private const val ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING = "allow_intended_submit_date_editing"
        
        fun newInstance(post: Post?, allowIntendedSubmitDateEditing: Boolean): NewpostFragment
        {
            val args = Bundle()

            if (post != null)
            {
                args.putSerializable(ARG_POST, post)
                args.putBoolean(ARG_NEW_POST, false)
            }
            else
            {
                args.putBoolean(ARG_NEW_POST, true)
            }

            args.putBoolean(ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING, allowIntendedSubmitDateEditing)

            val fragment = NewpostFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
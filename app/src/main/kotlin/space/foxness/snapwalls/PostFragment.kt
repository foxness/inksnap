package space.foxness.snapwalls

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.*
import space.foxness.snapwalls.Util.toast

class PostFragment : Fragment()
{
    private lateinit var viewPager: ViewPager
    private lateinit var adapter: PostFragmentPagerAdapter
    
    private lateinit var post: Post
    private var newPost = false
    private var allowIntendedSubmitDateEditing = false
    
    private val currentFragment get() = adapter.getItem(viewPager.currentItem)
    
    private class PostFragmentPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager)
    {
        private val fragmentList = mutableListOf<BasepostFragment>()

        override fun getItem(position: Int) = fragmentList[position]

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any)
        {
            super.destroyItem(container, position, `object`)
            fragmentList.removeAt(position)
        }

        override fun getCount() = fragmentList.size

        override fun getPageTitle(position: Int): CharSequence?
        {
            return when (position)
            {
                0 -> "self" // todo: extract
                1 -> "link"
                else -> throw Exception("how")
            }
        }
        
        fun addFragment(fragment: BasepostFragment) = fragmentList.add(fragment)
    }
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        
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
        
        val self = SelfpostFragment.newInstance(post, allowIntendedSubmitDateEditing)
        val link = LinkpostFragment.newInstance(post, allowIntendedSubmitDateEditing)
        
        adapter = PostFragmentPagerAdapter(childFragmentManager)
        adapter.addFragment(self)
        adapter.addFragment(link)
    }
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_newpost, container, false)
        
        viewPager = v.findViewById(R.id.newpost_viewpager)
        viewPager.adapter = adapter
        
//        val otsl = object : TabLayout.OnTabSelectedListener
//        {
//            override fun onTabSelected(tab: TabLayout.Tab)
//            {
//                log("tab ${tab.text} at ${tab.position} selected")
//            }
//            
//            override fun onTabReselected(tab: TabLayout.Tab)
//            {
//                log("tab ${tab.text} at ${tab.position} reselected")
//            }
//            
//            override fun onTabUnselected(tab: TabLayout.Tab)
//            {
//                log("tab ${tab.text} at ${tab.position} unselected")
//            }
//        }
//        
//        val tabLayout = v.findViewById<TabLayout>(R.id.newpost_tablayout)
//        tabLayout.addOnTabSelectedListener(otsl)
        
        return v
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_post, menu)

        val deleteItem = menu.findItem(R.id.menu_post_delete)
        deleteItem.isVisible = !newPost
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        return when (item!!.itemId)
        {
            R.id.menu_post_delete -> { deletePost(); true }
            R.id.menu_post_done -> { savePost(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun savePost()
    {
        unloadFragmentToPost()

        if (post.isValid(allowIntendedSubmitDateEditing))
        {
            val data = Intent()
            data.putExtra(RESULT_POST, post)
            activity!!.setResult(Activity.RESULT_OK, data)
            activity!!.finish()
        }
        else
        {
            toast(post.reasonWhyInvalid(allowIntendedSubmitDateEditing))
        }
    }

    private fun deletePost()
    {
        val i = Intent()
        i.putExtra(RESULT_DELETED_POST_ID, post.id)
        activity!!.setResult(RESULT_CODE_DELETED, i)
        activity!!.finish()
    }

    private fun unloadFragmentToPost()
    {
        post = currentFragment.getThePost()
    }
    
    companion object
    {
        private const val ARG_POST = "post"
        private const val ARG_NEW_POST = "new_post"
        private const val ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING = "allow_intended_submit_date_editing"

        private const val RESULT_POST = "post"
        private const val RESULT_DELETED_POST_ID = "deleted_post_id"

        const val RESULT_CODE_DELETED = 5

        // todo: have the activity have these too (for loose coupling)
        fun getPostFromResult(data: Intent) = data.getSerializableExtra(RESULT_POST) as Post

        fun getDeletedPostIdFromResult(data: Intent) = data.getStringExtra(RESULT_DELETED_POST_ID)!!
        
        fun newInstance(post: Post?, allowIntendedSubmitDateEditing: Boolean): PostFragment
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

            val fragment = PostFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
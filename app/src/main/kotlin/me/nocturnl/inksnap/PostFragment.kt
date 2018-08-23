package me.nocturnl.inksnap

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.view.*
import me.nocturnl.inksnap.Util.toast

class PostFragment : Fragment()
{
    private lateinit var viewPager: ViewPager
    private lateinit var adapter: PostFragmentPagerAdapter
    
    private lateinit var post: Post
    private lateinit var originalPost: Post
    
    var newPost = false 
        private set
    
    private var allowIntendedSubmitDateEditing = false
    
    private var currentTabIndex = 0
    
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
                TEXTPOST_TAB_INDEX -> "text" // todo: extract
                LINKPOST_TAB_INDEX -> "link"
                else -> throw Exception("how")
            }
        }
        
        fun addFragment(fragment: BasepostFragment) = fragmentList.add(fragment)
    }
    
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        
        val args = arguments!!
        if (args.getBoolean(ARG_NEW_POST))
        {
            newPost = true
            post = Post.newInstance()
        }
        else
        {
            post = args.getSerializable(ARG_POST) as Post
        }
        
        // todo: use data class & copy()
        
        originalPost = Post()
        originalPost.content = post.content
        originalPost.intendedSubmitDate = post.intendedSubmitDate
        originalPost.isLink = post.isLink
        originalPost.title = post.title
        originalPost.subreddit = post.subreddit
        
        // ---

        allowIntendedSubmitDateEditing = args.getBoolean(ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING)
        
        val self = TextpostFragment.newInstance(post, allowIntendedSubmitDateEditing)
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
        viewPager.currentItem = if (post.isLink) LINKPOST_TAB_INDEX else TEXTPOST_TAB_INDEX

        currentTabIndex = viewPager.currentItem
        
        val opcl = object : ViewPager.OnPageChangeListener
        {
            private var dragStarted = false

            private fun synchronizePost()
            {
                unloadFragmentToPost()
                
                val unselectedFragments = (0 until adapter.count)
                        .filter { it != currentTabIndex }
                        .map { adapter.getItem(it) }
                
                unselectedFragments.forEach {
                    it.applyPost(post)
                }
            }
            
            override fun onPageScrollStateChanged(state: Int)
            {
                when (state)
                {
                    ViewPager.SCROLL_STATE_DRAGGING ->
                    {
                        dragStarted = true
                        synchronizePost()
                    }
                    
                    ViewPager.SCROLL_STATE_SETTLING ->
                    {
                        // there is no drag when user presses on tab
                        // so we need to synchronize here
                        
                        if (!dragStarted)
                        {
                            synchronizePost()
                        }
                        
                        dragStarted = false
                    }
                }
            }

            override fun onPageScrolled(position: Int,
                                        positionOffset: Float,
                                        positionOffsetPixels: Int) { }

            override fun onPageSelected(position: Int)
            {
                currentTabIndex = position
            }
        }
        
        viewPager.addOnPageChangeListener(opcl)
        
        return v
    }
    
    fun areThereUnsavedChanges(): Boolean
    {
        unloadFragmentToPost()
        
        // todo: use data class && check equality with ==
        return originalPost.content != post.content 
               || originalPost.intendedSubmitDate != post.intendedSubmitDate 
               || originalPost.isLink != post.isLink 
               || originalPost.title != post.title 
               || originalPost.subreddit != post.subreddit
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
        val onDelete = { dialogInterface: DialogInterface, which: Int ->
            val i = Intent()
            i.putExtra(RESULT_DELETED_POST_ID, post.id)
            activity!!.setResult(RESULT_CODE_DELETED, i)
            activity!!.finish()
        }
        
        val onCancel = { dialogInterface: DialogInterface, which: Int ->
            dialogInterface.cancel()
        }
        
        val dialog = AlertDialog.Builder(context!!)
                .setMessage("Are you sure you want to delete this post?") // todo: extract
                .setPositiveButton("Delete", onDelete) // todo: extract
                .setNegativeButton(android.R.string.cancel, onCancel)
                .create()
        
        dialog.show()
    }

    private fun unloadFragmentToPost()
    {
        val selectedFragment = adapter.getItem(currentTabIndex)
        post = selectedFragment.getThePost()
    }
    
    companion object
    {
        private const val TEXTPOST_TAB_INDEX = 0
        private const val LINKPOST_TAB_INDEX = 1
        
        private const val ARG_POST = "post"
        private const val ARG_NEW_POST = "new_post"
        private const val ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING = "aisde"

        private const val RESULT_POST = "post"
        private const val RESULT_DELETED_POST_ID = "deleted_post_id"

        const val RESULT_CODE_DELETED = 5
        
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
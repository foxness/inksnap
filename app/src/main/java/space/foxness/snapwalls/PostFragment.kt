package space.foxness.snapwalls

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.webkit.URLUtil.isValidUrl
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import space.foxness.snapwalls.Util.toast

class PostFragment : Fragment() {
    
    private lateinit var titleEdit: EditText
    private lateinit var contentEdit: EditText
    private lateinit var subredditEdit: EditText
    private lateinit var typeSwitch: Switch

    private lateinit var queue: Queue
    private lateinit var post: Post
    private var newPost = false

    // todo: account for submitservice
    // example: submit service submits and deletes a post while you're editing it
    // bad stuff will happen then
    // this also applies to all other activities/fragments
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        queue = Queue.getInstance(context!!)
        
        val args = arguments!!
        if (args.getBoolean(ARG_NEW_POST)) {
            newPost = true
            post = Post()
            post.title = "testy" // todo: remove on production
            post.content = "besty" // same ^
            post.subreddit = "test" // same ^
        } else {
            val postId = args.getLong(ARG_POST_ID)
            post = queue.getPost(postId)!!
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_post, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.menu_post_delete -> {
                deletePost()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deletePost() {
        if (newPost) {
            clearResult()
        } else {
            queue.deletePost(post.id)
        }
            
        activity!!.finish()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_post, container, false)

        // TITLE EDIT -------------------------
        
        titleEdit = v.findViewById(R.id.post_title)
        titleEdit.setText(post.title)
        
        // CONTENT EDIT -----------------------

        contentEdit = v.findViewById(R.id.post_content)
        contentEdit.setText(post.content)
        
        // SUBREDDIT EDIT ---------------------

        subredditEdit = v.findViewById(R.id.post_subreddit)
        subredditEdit.setText(post.subreddit)
        
        // TYPE SWITCH ------------------------

        typeSwitch = v.findViewById(R.id.post_type)
        typeSwitch.isChecked = post.type
        
        // SAVE BUTTON ------------------------
        
        val saveButton = v.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            
            unloadViewsToPost()

            val notEmptyTitle = !post.title.isEmpty()
            val notEmptySubreddit = !post.subreddit.isEmpty()
            val validContent = !(post.type && !isValidUrl(post.content))

            val isPostValid = notEmptyTitle && validContent && notEmptySubreddit
            
            if (isPostValid) {
                
                if (newPost)
                    setNewPostResult()
                else
                    queue.updatePost(post)
                
                activity!!.finish()
                
            } else {
                toast(constructDenyMessage(notEmptyTitle, post.content, notEmptySubreddit, post.type))
            }
        }

        return v
    }
    
    private fun unloadViewsToPost() {
        post.title = titleEdit.text.toString()
        post.content = contentEdit.text.toString()
        post.subreddit = subredditEdit.text.toString()
        post.type = typeSwitch.isChecked
    }
    
    private fun setNewPostResult() {
        val data = Intent()
        data.putExtra(RESULT_NEW_POST, post)
        activity!!.setResult(Activity.RESULT_OK, data)
    }
    
    private fun clearResult() {
        activity!!.setResult(Activity.RESULT_CANCELED)
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_NEW_POST = "new_post"
        private const val RESULT_NEW_POST = "new_post"
        
        private fun constructDenyMessage(notEmptyTitle: Boolean,
                                         content: String,
                                         notEmptySubreddit: Boolean,
                                         type: Boolean): String {
            if (!notEmptyTitle)
                return "Title must not be empty"
            
            if (!notEmptySubreddit)
                return "Subreddit must not be empty"
            
            if (type && !isValidUrl(content))
                return "Url must be valid"
            
            throw Exception("Bad logic somewhere in SaveButton.Click") // this should never happen
        }
        
        fun getNewPostFromResult(data: Intent)
                = data.getSerializableExtra(RESULT_NEW_POST) as? Post

        fun newInstance(postId: Long): PostFragment {
            val args = Bundle()
            args.putLong(ARG_POST_ID, postId)
            args.putBoolean(ARG_NEW_POST, false)

            val fragment = PostFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(): PostFragment {
            val args = Bundle()
            args.putBoolean(ARG_NEW_POST, true)

            val fragment = PostFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
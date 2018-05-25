package space.foxness.snapwalls

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Switch

class PostFragment : Fragment() {

    private lateinit var queue: Queue
    private lateinit var post: Post
    private var isValidPost = false // todo: don't let user create invalid posts
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

    private fun deletePost() { // todo: clear activity result if newpost
        if (!newPost)
            queue.deletePost(post.id)
        activity!!.finish()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_post, container, false)

        // TITLE EDIT -------------------------
        
        val titleEdit = v.findViewById<EditText>(R.id.post_title)
        titleEdit.setText(post.title)
        titleEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                post.title = s.toString()
                updateIsValidPost()
            }

            override fun afterTextChanged(s: Editable) { }
        })
        
        // CONTENT EDIT -----------------------

        val contentEdit = v.findViewById<EditText>(R.id.post_content)
        contentEdit.setText(post.content)
        contentEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                post.content = s.toString()
                updateIsValidPost()
            }

            override fun afterTextChanged(s: Editable) { }
        })
        
        // SUBREDDIT EDIT ---------------------

        val subredditEdit = v.findViewById<EditText>(R.id.post_subreddit)
        subredditEdit.setText(post.subreddit)
        subredditEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                post.subreddit = s.toString()
                updateIsValidPost()
            }

            override fun afterTextChanged(s: Editable) { }
        })
        
        // TYPE SWITCH ------------------------

        val typeSwitch = v.findViewById<Switch>(R.id.post_type)
        typeSwitch.isChecked = post.type
        typeSwitch.setOnCheckedChangeListener { _, isChecked -> post.type = isChecked }
        
        // SAVE BUTTON ------------------------
        
        val saveButton = v.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {  // todo: refuse to save an invalid post
            if (newPost) {
                setNewPostResult()
            } else {
                queue.updatePost(post)
            }
        }

        return v
    }
    
    private fun setNewPostResult() {
        val data = Intent()
        data.putExtra(RESULT_NEW_POST, post)
        activity!!.setResult(Activity.RESULT_OK, data)
    }

    private fun updateIsValidPost() {
        val validTitle = !post.title.isEmpty()
        val validContent = !(post.type && post.content.isEmpty()) // todo: add url validation
        val validSubreddit = !post.subreddit.isEmpty()

        isValidPost = validTitle && validContent && validSubreddit
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_NEW_POST = "new_post"
        private const val RESULT_NEW_POST = "new_post"
        
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
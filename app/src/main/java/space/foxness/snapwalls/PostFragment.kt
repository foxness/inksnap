package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.Switch

class PostFragment : Fragment() {

    private lateinit var queue: Queue
    private lateinit var post: Post
    private var isValidPost: Boolean = false // todo: don't let user create invalid posts

    // todo: account for submitservice
    // example: submit service submits and deletes a post while you're editing it
    // bad stuff will happen then
    // this also applies to all other activities/fragments
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val postId = arguments!!.getLong(ARG_POST_ID)
        queue = Queue.getInstance(context!!)
        post = queue.getPost(postId)!!
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
        queue.deletePost(post.id)
        activity?.finish()
    }

    override fun onPause() {
        super.onPause()
        queue.updatePost(post)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_post, container, false)

        val titleET = v.findViewById<EditText>(R.id.post_title)
        
        titleET.setText(post.title)
        
        // TODO: REMOVE ON PRODUCTION ---------------
        val title = if (post.title.isEmpty()) "testy" else post.title
        post.title = title
        titleET.setText(title)
        // ------------------------------------------
        
        titleET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                post.title = s.toString()
                updateIsValidPost()
            }

            override fun afterTextChanged(s: Editable) { }
        })

        val contentET = v.findViewById<EditText>(R.id.post_content)
        
        contentET.setText(post.content)

        // TODO: REMOVE ON PRODUCTION ---------------
        val content = if (post.content.isEmpty()) "besty" else post.content
        post.content = content
        contentET.setText(content)
        // ------------------------------------------
        
        contentET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                post.content = s.toString()
                updateIsValidPost()
            }

            override fun afterTextChanged(s: Editable) { }
        })

        val subredditET = v.findViewById<EditText>(R.id.post_subreddit)
        subredditET.setText(post.subreddit)
        subredditET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                post.subreddit = s.toString()
                updateIsValidPost()
            }

            override fun afterTextChanged(s: Editable) { }
        })

        val typeSwitch = v.findViewById<Switch>(R.id.post_type)
        typeSwitch.isChecked = post.type
        typeSwitch.setOnCheckedChangeListener { _, isChecked -> post.type = isChecked }

        return v
    }

    private fun updateIsValidPost() {
        val validTitle = !post.title.isEmpty()
        val validContent = !(post.type && post.content.isEmpty())
        val validSubreddit = !post.subreddit.isEmpty()

        isValidPost = validTitle && validContent && validSubreddit
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: Long): PostFragment {
            val args = Bundle()
            args.putLong(ARG_POST_ID, postId)

            val fragment = PostFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
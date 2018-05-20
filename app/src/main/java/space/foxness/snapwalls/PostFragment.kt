package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.Switch

import java.util.UUID

class PostFragment : Fragment() {

    private lateinit var post: Post
    private var isValidPost: Boolean = false // todo: don't let user create invalid posts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val postId = arguments!!.getSerializable(ARG_POST_ID) as UUID
        post = Queue.getInstance(context!!).getPost(postId)
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
        Queue.getInstance(context!!).deletePost(post.id)
        activity?.finish()
    }

    override fun onPause() {
        super.onPause()
        Queue.getInstance(context!!).updatePost(post)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_post, container, false)

        val titleET = v.findViewById<EditText>(R.id.post_title)
        titleET.setText(post.title)
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
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                post.subreddit = s.toString()
                updateIsValidPost()
            }

            override fun afterTextChanged(s: Editable) {

            }
        })

        val typeSwitch = v.findViewById<Switch>(R.id.post_type)
        typeSwitch.isChecked = post.type
        typeSwitch.setOnCheckedChangeListener { _, isChecked -> post.type = isChecked }

        return v
    }

    private fun updateIsValidPost() {
        val validTitle = !post.title.isEmpty()
        val validContent = !post.content.isEmpty()
        val validSubreddit = !post.subreddit.isEmpty()

        isValidPost = validTitle && validContent && validSubreddit
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: UUID): PostFragment {
            val args = Bundle()
            args.putSerializable(ARG_POST_ID, postId)

            val fragment = PostFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
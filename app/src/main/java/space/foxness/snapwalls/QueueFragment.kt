package space.foxness.snapwalls

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import java.util.*

class QueueFragment : Fragment(), Reddit.Callbacks {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private lateinit var signinMenuItem: MenuItem

    private var reddit = Reddit(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        
        restoreConfig()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_queue, container, false)

        recyclerView = v.findViewById(R.id.queue_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(activity)

        val posts = Queue.getInstance(activity!!).posts
        adapter = PostAdapter(posts)
        recyclerView.adapter = adapter
        
        updateUI()

        return v
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        adapter.setPosts(Queue.getInstance(activity!!).posts)
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_queue, menu)

        signinMenuItem = menu!!.findItem(R.id.menu_queue_signin)

        updateMenu()
    }

    private fun addNewPost() {
        val s = Post()
        s.subreddit = "test" // todo: change this
        Queue.getInstance(activity!!).addPost(s)
        startActivity(PostPagerActivity.newIntent(activity!!, s.id))
    }

    private fun showSigninDialog() {
        signinMenuItem.isEnabled = false
        // we need ^ this because there's a token doesn't arrive immediately after the dialog is dismissed
        // and the user should not be able to press it when the token is being fetched

        val authDialog = Dialog(activity!!)
        authDialog.setContentView(R.layout.dialog_auth)
        authDialog.setOnCancelListener { signinMenuItem.isEnabled = true }

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)
        authWebview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (reddit.tryExtractCode(url)) {
                    authDialog.dismiss()
                    reddit.fetchAuthTokens()
                }
            }
        }

        authWebview.loadUrl(reddit.authorizationUrl)
        authDialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.menu_queue_add -> {
                addNewPost()
                true
            }
            R.id.menu_queue_signin -> {
                showSigninDialog()
                true
            }
            R.id.menu_queue_submit -> {
                submitTopPost()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun submitTopPost() {
        val posts = Queue.getInstance(activity!!).posts
        if (posts.isEmpty())
        {
            Toast.makeText(activity, "No post to submit", Toast.LENGTH_SHORT).show()
            return
        }
        
        submit(posts.first())
    }

    private fun submit(s: Post) {
        if (!reddit.canSubmitRightNow) {
            Toast.makeText(activity, "Can't submit right now", Toast.LENGTH_SHORT).show()
            return
        }
        
        reddit.submit(s)
    }

    override fun onTokenFetchFinish() {
        Toast.makeText(activity, "fetched tokens, can post? " + reddit.canSubmitRightNow, Toast.LENGTH_SHORT).show()
        updateMenu()
    }

    private fun updateMenu() {
        signinMenuItem.isEnabled = !reddit.isSignedIn
    }

    override fun onNewParams() {
        saveConfig()
        Toast.makeText(activity, "SAVED THE CONFIG", Toast.LENGTH_SHORT).show()
    }

    override fun onSubmit(link: String) {
        Toast.makeText(activity, "GOT LINK: $link", Toast.LENGTH_SHORT).show()
    }

    private fun saveConfig() {
        val rp = reddit.params
        val expirationDate = if (rp.accessTokenExpirationDate == null) CONFIG_NULL_SUBSTITUTE else rp.accessTokenExpirationDate!!.time
        val lastSubmissionDate = if (rp.lastSubmissionDate == null) CONFIG_NULL_SUBSTITUTE else rp.lastSubmissionDate!!.time

        val sp = activity!!.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
        sp.edit()
                .putString(CONFIG_ACCESS_TOKEN, rp.accessToken)
                .putString(CONFIG_REFRESH_TOKEN, rp.refreshToken)
                .putLong(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, expirationDate)
                .putLong(CONFIG_LAST_SUBMISSION_DATE, lastSubmissionDate)
                .apply()
    }

    private fun restoreConfig() {
        val rp = Reddit.Params()

        val sp = activity!!.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
        rp.accessToken = sp.getString(CONFIG_ACCESS_TOKEN, null)
        rp.refreshToken = sp.getString(CONFIG_REFRESH_TOKEN, null)

        var dateInMs: Long? = sp.getLong(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, CONFIG_NULL_SUBSTITUTE)
        rp.accessTokenExpirationDate = if (dateInMs == CONFIG_NULL_SUBSTITUTE) null else Date(dateInMs!!)

        dateInMs = sp.getLong(CONFIG_LAST_SUBMISSION_DATE, CONFIG_NULL_SUBSTITUTE)
        rp.lastSubmissionDate = if (dateInMs == CONFIG_NULL_SUBSTITUTE) null else Date(dateInMs)

        reddit.params = rp
    }

    private inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView
        private val contentTextView: TextView
        private val typeCheckBox: CheckBox

        private lateinit var post: Post

        init {
            itemView.setOnClickListener({
                val i = PostPagerActivity.newIntent(activity!!, post.id)
                startActivity(i)
            })

            titleTextView = itemView.findViewById(R.id.queue_post_title)
            contentTextView = itemView.findViewById(R.id.queue_post_content)
            typeCheckBox = itemView.findViewById(R.id.queue_post_type)
        }

        fun bindPost(p: Post) {
            post = p
            titleTextView.text = post.title
            contentTextView.text = post.content
            typeCheckBox.isChecked = post.type
        }
    }

    private inner class PostAdapter(private var posts: List<Post>) : RecyclerView.Adapter<PostHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
            val inflater = LayoutInflater.from(activity)
            val v = inflater.inflate(R.layout.queue_post, parent, false)
            return PostHolder(v)
        }

        override fun onBindViewHolder(holder: PostHolder, position: Int) {
            val s = posts[position]
            holder.bindPost(s)
        }

        fun setPosts(posts_: List<Post>) {
            posts = posts_
        }

        override fun getItemCount() = posts.size
    }

    companion object {
        private const val CONFIG_ACCESS_TOKEN = "accessToken"
        private const val CONFIG_REFRESH_TOKEN = "refreshToken"
        private const val CONFIG_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate"
        private const val CONFIG_LAST_SUBMISSION_DATE = "lastSubmissionDate"

        private const val CONFIG_NULL_SUBSTITUTE: Long = 0
    }
}

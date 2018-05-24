package space.foxness.snapwalls

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.TextView
import android.widget.ToggleButton
import com.github.debop.kodatimes.times
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import org.joda.time.Duration
import space.foxness.snapwalls.Util.log
import space.foxness.snapwalls.Util.toNice
import space.foxness.snapwalls.Util.toast

class QueueFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private lateinit var signinMenuItem: MenuItem
    private lateinit var timerText: TextView
    
    private var timerCountdown: CountDownTimer? = null
    
    private lateinit var reddit: Reddit
    private lateinit var postScheduler: PostScheduler

    private val initialDelay = Duration.standardSeconds(37)
    private val period = Duration.standardHours(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        
        reddit = Autoreddit.getInstance(context!!).reddit
        postScheduler = PostScheduler(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_queue, container, false)

        val tb = v.findViewById<ToggleButton>(R.id.queue_toggle)
        tb.setOnCheckedChangeListener { button, isChecked ->
            button.isEnabled = false
            toggleAutoSubmission(isChecked)
            button.isEnabled = true
        }
        
        recyclerView = v.findViewById(R.id.queue_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val posts = Queue.getInstance(context!!).posts
        adapter = PostAdapter(posts)
        recyclerView.adapter = adapter

        timerText = v.findViewById(R.id.queue_timer)
        setupTimer()
        
        updateUI()

        return v
    }
    
    private fun setupTimer() {
        
        val updateTimer: (Duration) -> Unit = { timerText.text = it.toNice() }
        
        if (Config.getInstance(context!!).scheduled) {
            timerCountdown = object: CountDownTimer(Duration(DateTime.now(), 
                    Queue.getInstance(context!!).posts.first().scheduledDate!!).millis, 
                    1000) {

                // todo: something?
                override fun onFinish() { }

                override fun onTick(millisUntilFinished: Long) {
                    updateTimer(Duration(millisUntilFinished))
                }
            }
            
            timerCountdown!!.start()
        } else {
            timerCountdown?.cancel()
            updateTimer(initialDelay)
        }
    }
    
    private fun toggleAutoSubmission(on: Boolean) {
        val queue = Queue.getInstance(context!!)
        val config = Config.getInstance(context!!)
        val posts = queue.posts
        if (on) {
            if (!reddit.isSignedIn) {
                toast("You must be signed in to do that")
                return
            }

            if (posts.isEmpty()) {
                toast("No posts to submit")
                return
            }
            
            val postDelays = HashMap(posts
                    .mapIndexed { i, post -> post.id to initialDelay + period * i.toLong() }
                    .toMap())

            val now = DateTime.now()
            posts.forEach {
                it.scheduledDate = now + postDelays[it.id]
                queue.updatePost(it)
            }
            
            config.scheduled = true
            
            postScheduler.scheduleDelayedPosts(postDelays)
            log("SCHEDULED")
            
        } else {
            posts.forEach {
                it.scheduledDate = null
                queue.updatePost(it)
            }

            config.scheduled = false
            
            postScheduler.cancelScheduledPosts(posts.map { it.id })
            log("CANCELED")
        }

        setupTimer()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        adapter.setPosts(Queue.getInstance(context!!).posts)
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_queue, menu)

        signinMenuItem = menu!!.findItem(R.id.menu_queue_signin)

        updateMenu()
    }

    private fun addNewPost() {

        // todo: schedule new posts automatically if autosubmit is on
        
        val p = Post()
        p.subreddit = "test" // todo: change this
        p.id = Queue.getInstance(context!!).addPost(p)
        startActivity(PostPagerActivity.newIntent(context!!, p.id))
    }

    private fun showSigninDialog() {
        signinMenuItem.isEnabled = false
        // we need ^ this because there's a token doesn't arrive immediately after the dialog is dismissed
        // and the user should not be able to press it when the token is being fetched

        val authDialog = Dialog(context!!)
        authDialog.setContentView(R.layout.dialog_auth)
        authDialog.setOnCancelListener { signinMenuItem.isEnabled = true }

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)
        authWebview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (reddit.tryExtractCode(url)) {
                    authDialog.dismiss()

                    doAsync {
                        reddit.fetchAuthTokens({ error ->
                            uiThread {
                                if (error != null)
                                    throw error

                                toast(if (reddit.canSubmitRightNow) "Success" else "Fail")
                                updateMenu()
                            }
                        })
                    }
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
        val posts = Queue.getInstance(context!!).posts
        if (posts.isEmpty()) {
            toast("No post to submit")
            return
        }

        if (!reddit.canSubmitRightNow) {
            toast("Can't submit right now")
            return
        }

        reddit.submit(posts.first(), { error, link ->
            if (error != null)
                throw error

            toast("GOT LINK: $link")
        })
    }

    private fun updateMenu() {
        signinMenuItem.isEnabled = !reddit.isSignedIn
    }

    private inner class PostAdapter(private var posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostHolder>() {

        private inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView
            private val contentTextView: TextView
            private val typeCheckBox: CheckBox

            private lateinit var post: Post

            init {
                itemView.setOnClickListener({
                    val i = PostPagerActivity.newIntent(context!!, post.id)
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
            val inflater = LayoutInflater.from(context)
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
}
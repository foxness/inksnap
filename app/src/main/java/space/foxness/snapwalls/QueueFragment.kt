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
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
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
    private lateinit var timerToggle: Button
    
    private lateinit var timerObject: CountDownTimer

    private val initialDelay = Duration.standardSeconds(37)
    private val period = Duration.standardHours(3)
    
    private lateinit var reddit: Reddit
    private lateinit var postScheduler: PostScheduler
    
    private var timeLeft: Duration = initialDelay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        
        reddit = Autoreddit.getInstance(context!!).reddit
        postScheduler = PostScheduler(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_queue, container, false)

        val autosubmitEnabled = Config.getInstance(context!!).autosubmitEnabled
        val posts = Queue.getInstance(context!!).posts

        // RECYCLER VIEW ------------------------------

        recyclerView = v.findViewById(R.id.queue_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = PostAdapter(posts)
        recyclerView.adapter = adapter

        updatePostList()
        
        // TIMER TOGGLE -------------------------------

        timerToggle = v.findViewById(R.id.queue_toggle)
        updateTimerToggleText(!autosubmitEnabled)
        timerToggle.setOnClickListener { button ->
            button.isEnabled = false
            val autosubmitOn = Config.getInstance(context!!).autosubmitEnabled
            toggleAutosubmit(!autosubmitOn)
            button.isEnabled = true
        }
        
        // TIMER --------------------------------------

        timerText = v.findViewById(R.id.queue_timer)
        
        if (autosubmitEnabled) {
            val unpausedTimeLeft = Duration(DateTime.now(), posts.first().scheduledDate!!)
            timerObject = getTimerObject(unpausedTimeLeft)
            timerObject.start()
        } else {
            updateTimerText(timeLeft)
        }

        return v
    }
    
    private fun updateTimerToggleText(turnOn: Boolean) {
        timerToggle.text = if (turnOn) "Turn on" else "Turn off"
    }

    // todo: properties for config & queue?
    private fun toggleAutosubmit(on: Boolean) {
        
        // autosubmit behavior:
        // can't turn it on if there are no posts to submit
        // it turns off when all scheduled posts are submitted todo
        
        val queue = Queue.getInstance(context!!)
        val config = Config.getInstance(context!!)
        val posts = queue.posts
        
        if (on == config.autosubmitEnabled) // this should never happen
            throw RuntimeException("Can't change autosubmit to state it's already in")
        
        if (on) {
            if (!reddit.isSignedIn) {
                toast("You must be signed in to autosubmit")
                return
            }

            if (posts.isEmpty()) {
                toast("No posts to autosubmit")
                return
            }
            
            val postDelays = HashMap(posts
                    .mapIndexed { i, post -> post.id to timeLeft + period * i.toLong() }
                    .toMap())
            
            timerObject = getTimerObject(timeLeft)
            timerObject.start()
            
            postScheduler.scheduleDelayedPosts(postDelays)

            val now = DateTime.now()
            posts.forEach {
                it.scheduledDate = now + postDelays[it.id]
                queue.updatePost(it)
            }

            config.autosubmitEnabled = true
            
            log("Scheduled ${posts.size} post(s)")
            
        } else {
            timerObject.cancel()
            timeLeft = Duration(DateTime.now(), posts.first().scheduledDate!!)
            updateTimerText(timeLeft) // a potentially useless statement because of the timer's last update...
            
            postScheduler.cancelScheduledPosts(posts.map { it.id })
            
            posts.forEach {
                it.scheduledDate = null
                queue.updatePost(it)
            }

            config.autosubmitEnabled = false

            log("Canceled ${posts.size} scheduled post(s)")
        }

        updateTimerToggleText(!on)
    }
    
    private fun getTimerObject(timeLeft: Duration): CountDownTimer {
        return object : CountDownTimer(timeLeft.millis, TIMER_UPDATE_INTERVAL_MS) {
            // todo: something?
            override fun onFinish() {}

            override fun onTick(millisUntilFinished: Long) {
//                log("TICK")
                updateTimerText(Duration(millisUntilFinished))
            }
        }
    }
    
    private fun updateTimerText(timeLeft: Duration) {
        timerText.text = timeLeft.toNice()
    }

    // todo: onPause (NOT ONRESUME) stop the timer ticking because the timer is not being seen
    // todo: resume ticking in onResume
    override fun onResume() {
        super.onResume()
        updatePostList()
    }

    private fun updatePostList() {
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

        // todo: schedule new posts automatically if autosubmitEnabled is on
        
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
    
    companion object {
        private const val TIMER_UPDATE_INTERVAL_MS: Long = 1000 // 1 second
    }
}
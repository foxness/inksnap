package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.preference.PreferenceManager
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.Duration
import space.foxness.snapwalls.Queue.Companion.earliest
import space.foxness.snapwalls.Queue.Companion.onlyScheduled
import space.foxness.snapwalls.Util.log
import space.foxness.snapwalls.Util.randomState
import space.foxness.snapwalls.Util.timeLeftUntil
import space.foxness.snapwalls.Util.toNice
import space.foxness.snapwalls.Util.toast

class ManualQueueFragment : QueueFragment()
{
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private lateinit var timerText: TextView
    private lateinit var timerToggle: Button

    private lateinit var timerObject: CountDownTimer

    private lateinit var settingsManager: SettingsManager
    private lateinit var queue: Queue
    private lateinit var reddit: Reddit
    private lateinit var imgurAccount: ImgurAccount

    private lateinit var postScheduler: PostScheduler

    private var redditTokenFetching = false

    private var receiverRegistered = false

    private val submitReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?)
        {
            toast("post submitted :O")

            val scheduledPosts = queue.posts.onlyScheduled()

            if (scheduledPosts.isEmpty())
            {
                updateTimerText(null)
            }
            else
            {
                val timeLeft = timeLeftUntil(scheduledPosts.earliest()!!.intendedSubmitDate!!)
                startTimer(timeLeft)
            }

            updatePostList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // todo: to be more efficient don't write to config on every change
        // write like in onPause or something
        // im talking mostly about config.timeLeft and config.autosubmitEnabled

        val ctx = context!!
        settingsManager = SettingsManager.getInstance(ctx)
        queue = Queue.getInstance(ctx)
        postScheduler = PostScheduler.getInstance(ctx)
        reddit = Autoreddit.getInstance(ctx).reddit
        imgurAccount = Autoimgur.getInstance(ctx).imgurAccount

        PreferenceManager.setDefaultValues(ctx, R.xml.preferences, false)
    }

    private fun initUi()
    {
        // RECYCLER VIEW ------------------------------

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context,
                                                             DividerItemDecoration.VERTICAL))

        adapter = PostAdapter(queue.posts)
        recyclerView.adapter = adapter

        // TIMER TOGGLE -------------------------------

        timerToggle.setOnClickListener { button ->
            button.isEnabled = false
            toggleAutosubmit(!settingsManager.autosubmitEnabled)
            button.isEnabled = true
        }
    }

    private fun registerSubmitReceiver()
    {
        if (receiverRegistered)
        {
            return
        }

        receiverRegistered = true

        val lbm = LocalBroadcastManager.getInstance(activity!!)
        val intentFilter = IntentFilter(AutosubmitService.POST_SUBMITTED)
        lbm.registerReceiver(submitReceiver, intentFilter)
    }

    private fun unregisterSubmitReceiver()
    {
        if (!receiverRegistered)
        {
            return
        }

        receiverRegistered = false

        val lbm = LocalBroadcastManager.getInstance(activity!!)
        lbm.unregisterReceiver(submitReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val v = inflater.inflate(R.layout.fragment_queue_manual, container, false)

        recyclerView = v.findViewById(R.id.queue_recyclerview)
        timerToggle = v.findViewById(R.id.queue_toggle)
        timerText = v.findViewById(R.id.queue_timer)

        initUi()

        return v
    }

    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(autosubmitEnabled: Boolean) // todo: refactor to not use arg
    {
        timerToggle.text = if (autosubmitEnabled) "Turn off" else "Turn on"

        var timeLeft: Duration? = null

        val earliestPost = queue.posts.onlyScheduled().earliest()
        if (autosubmitEnabled && earliestPost != null)
        {
            timeLeft = timeLeftUntil(earliestPost.intendedSubmitDate!!)
        }

        updateTimerText(timeLeft)
    }

    private fun toggleAutosubmit(on: Boolean)
    {
        if (on == settingsManager.autosubmitEnabled) // this should never happen
        {
            throw RuntimeException("Can't change autosubmit to state it's already in")
        }

        if (on)
        {
            if (!reddit.isLoggedIn)
            {
                toast("You must be signed in to autosubmit")
                return
            }

            settingsManager.autosubmitEnabled = true

            val manualPosts = queue.posts.filter { it.intendedSubmitDate != null }

            if (manualPosts.isNotEmpty())
            {
                val earliestPost = manualPosts.earliest()!!
                val timeLeft = timeLeftUntil(earliestPost.intendedSubmitDate!!)

                startTimerAndRegisterReceiver(timeLeft)

                postScheduler.scheduleManualPosts(manualPosts)

                log("Scheduled ${manualPosts.size} post(s)")
            }
        }
        else
        {
            settingsManager.autosubmitEnabled = false
            
            val scheduledPosts = queue.posts.onlyScheduled()

            if (scheduledPosts.isNotEmpty())
            {
                unregisterSubmitReceiver()

                timerObject.cancel()

                postScheduler.cancelScheduledPosts(scheduledPosts.reversed()) // ...its for optimization

                log("Canceled ${scheduledPosts.size} scheduled post(s)")
            }
        }

        updateToggleViews(on)
    }

    private fun updateTimerText(timeLeft: Duration?)
    {
        timerText.text = timeLeft?.toNice() ?: "---"
    }

    override fun onStart()
    {
        super.onStart()

        toast("I am Manual")

        // TODO: schedule posts that were added a date if autosubmit is on and type is manual

        // assume period and autosubmit type never change while autosubmit is enabled
        // todo: actually prohibit changing these values while autosubmit is on

        if (settingsManager.timeLeft == null)
        {
            settingsManager.timeLeft = settingsManager.period
        }

        val scheduledPosts = queue.posts.onlyScheduled()
        if (scheduledPosts.isNotEmpty())
        {
            val timeLeft = timeLeftUntil(scheduledPosts.earliest()!!.intendedSubmitDate!!)
            startTimerAndRegisterReceiver(timeLeft)
        }

        updateToggleViews(settingsManager.autosubmitEnabled)
        updatePostList()
    }

    private fun startTimer(timeLeft: Duration)
    {
        fun getTimerObject(timeLeft_: Duration): CountDownTimer
        {
            return object : CountDownTimer(timeLeft_.millis, TIMER_UPDATE_INTERVAL_MS)
            {
                override fun onFinish()
                {
                    // todo: remove the submitted item from post list
                }

                override fun onTick(millisUntilFinished: Long)
                {
                    updateTimerText(Duration(millisUntilFinished))
                }
            }
        }

        timerObject = getTimerObject(timeLeft)
        timerObject.start()
    }

    private fun startTimerAndRegisterReceiver(timeLeft: Duration)
    {
        startTimer(timeLeft)
        registerSubmitReceiver()
    }

    override fun onStop()
    {
        super.onStop()

        val scheduledPosts = queue.posts.onlyScheduled()
        if (scheduledPosts.isNotEmpty())
        {
            timerObject.cancel()
        }

        unregisterSubmitReceiver() // maybe move this into the if?
    }

    private fun updatePostList()
    {
        adapter.setPosts(queue.posts)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?)
    {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_queue, menu)

        val redditLoginMenuItem = menu!!.findItem(R.id.menu_queue_reddit_login)!!
        val imgurLoginMenuItem = menu.findItem(R.id.menu_queue_imgur_login)!!

        redditLoginMenuItem.isEnabled = !redditTokenFetching && !reddit.isLoggedIn
        imgurLoginMenuItem.isEnabled = !imgurAccount.isLoggedIn
    }

    private fun createNewPost()
    {
        val i = NewPostActivity.newIntent(context!!, true)
        startActivityForResult(i, REQUEST_CODE_NEW_POST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        if (resultCode != Activity.RESULT_OK)
        {
            return
        }

        if (requestCode == REQUEST_CODE_NEW_POST)
        {
            val newPost = PostFragment.getNewPostFromResult(data!!)!!

            queue.addPost(newPost)

            if (settingsManager.autosubmitEnabled && newPost.intendedSubmitDate != null)
            {
                postScheduler.schedulePost(newPost)
            }
        }
    }

    private fun showRedditLoginDialog()
    {
        val authDialog = Dialog(context!!)
        authDialog.setContentView(R.layout.dialog_auth)

        authDialog.setOnCancelListener { toast("Fail") }

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)
        authWebview.webViewClient = object : WebViewClient()
        {
            override fun onPageFinished(view: WebView, url: String)
            {
                super.onPageFinished(view, url)

                if (reddit.tryExtractCode(url))
                {
                    redditTokenFetching = true
                    activity!!.invalidateOptionsMenu()

                    authDialog.dismiss()

                    doAsync {
                        reddit.fetchAuthTokens()

                        uiThread {
                            redditTokenFetching = false
                            activity!!.invalidateOptionsMenu()
                            toast(if (reddit.isLoggedIn) "Success" else "Fail")
                        }
                    }
                }
            }
        }

        authWebview.loadUrl(reddit.authorizationUrl)
        authDialog.show()
    }

    private fun openSettings()
    {
        val i = SettingsActivity.newIntent(context!!)
        startActivity(i)
    }

    private fun testButton()
    {
        toast(randomState())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        return when (item!!.itemId)
        {
            R.id.menu_queue_add ->
            {
                createNewPost()
                true
            }
            R.id.menu_queue_test ->
            {
                testButton()
                true
            }
            R.id.menu_queue_reddit_login ->
            {
                showRedditLoginDialog()
                true
            }
            R.id.menu_queue_imgur_login ->
            {
                showImgurLoginDialog()
                true
            }
            R.id.menu_queue_settings ->
            {
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showImgurLoginDialog()
    {
        val authDialog = Dialog(context!!)
        authDialog.setContentView(R.layout.dialog_auth)

        authDialog.setOnDismissListener {
            activity!!.invalidateOptionsMenu()
            toast(if (imgurAccount.isLoggedIn) "Success" else "Fail")
        }

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)
        authWebview.webViewClient = object : WebViewClient()
        {
            override fun onPageFinished(view: WebView, url: String)
            {
                super.onPageFinished(view, url)

                if (imgurAccount.tryExtractTokens(url))
                {
                    authDialog.dismiss()
                }
            }
        }

        authWebview.loadUrl(imgurAccount.authorizationUrl)
        authDialog.show()
    }

    private inner class PostAdapter(private var posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostHolder>()
    {
        private inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        {
            private val titleTextView: TextView
            private val contentTextView: TextView
            private val typeCheckBox: CheckBox

            private lateinit var post: Post

            init
            {
                itemView.setOnClickListener {
                    val i = PostPagerActivity.newIntent(context!!, post.id, true)
                    startActivity(i)
                }

                titleTextView = itemView.findViewById(R.id.queue_post_title)
                contentTextView = itemView.findViewById(R.id.queue_post_content)
                typeCheckBox = itemView.findViewById(R.id.queue_post_type)
            }

            fun bindPost(p: Post)
            {
                post = p
                titleTextView.text = post.title
                contentTextView.text = post.content
                typeCheckBox.isChecked = post.type
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder
        {
            val inflater = LayoutInflater.from(context)
            val v = inflater.inflate(R.layout.queue_post, parent, false)
            return PostHolder(v)
        }

        override fun onBindViewHolder(holder: PostHolder, position: Int)
        {
            val post = posts[position]
            holder.bindPost(post)
        }

        fun setPosts(posts_: List<Post>)
        {
            posts = posts_
            notifyDataSetChanged()
        }

        override fun getItemCount() = posts.size
    }

    companion object
    {
        private const val TIMER_UPDATE_INTERVAL_MS: Long = 100 // 0.1 seconds
        private const val REQUEST_CODE_NEW_POST = 0
    }
}
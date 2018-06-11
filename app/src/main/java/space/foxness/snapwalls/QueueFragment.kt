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
import android.support.v4.app.Fragment
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
import android.widget.SeekBar
import android.widget.TextView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import org.joda.time.Duration
import space.foxness.snapwalls.Util.log
import space.foxness.snapwalls.Util.randomState
import space.foxness.snapwalls.Util.toNice
import space.foxness.snapwalls.Util.toast

class QueueFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private lateinit var timerText: TextView
    private lateinit var timerToggle: Button
    private lateinit var seekBar: SeekBar
    
    private lateinit var timerObject: CountDownTimer
    
    private lateinit var settingsManager: SettingsManager
    private lateinit var queue: Queue
    private lateinit var reddit: Reddit
    private lateinit var imgurAccount: ImgurAccount
    
    private lateinit var postScheduler: PostScheduler
    
    private var redditTokenFetching = false
    
    private var receiverRegistered = false
    
    private val submitReceiver = object: BroadcastReceiver() {
        
        override fun onReceive(context: Context?, intent: Intent?) {
            
            toast("post submitted :O")
            handleEnabledAutosubmit()
            updatePostList()
        }
    }
    
    private val allowScheduledDateEditing get() = settingsManager.autosubmitType == SettingsManager.AutosubmitType.Manual
    
    private fun handleEnabledAutosubmit() {
        
        if (queue.posts.isEmpty()) {
            settingsManager.autosubmitEnabled = false
            settingsManager.timeLeft = settingsManager.period
            updateToggleViews(false)
            
            unregisterSubmitReceiver()
        } else {
            val unpausedTimeLeft = Duration(DateTime.now(), queue.posts.first().scheduledDate!!)
            timerObject = getTimerObject(unpausedTimeLeft)
            timerObject.start()
            
            registerSubmitReceiver()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
    
    private fun initUi() {
        
        // RECYCLER VIEW ------------------------------

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))

        adapter = PostAdapter(queue.posts)
        recyclerView.adapter = adapter

        // TIMER TOGGLE -------------------------------

        timerToggle.setOnClickListener { button ->
            button.isEnabled = false
            toggleAutosubmit(!settingsManager.autosubmitEnabled)
            button.isEnabled = true
        }

        // SEEKBAR ------------------------------------

        seekBar.max = SEEKBAR_MAX_VALUE
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            private var timeLeft: Duration = Duration.ZERO

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return

                val percentage = 1 - progress.toDouble() / SEEKBAR_MAX_VALUE
                val millis = settingsManager.period.millis * percentage
                val rounded = Math.round(millis)
                timeLeft = Duration(rounded)
                updateTimerText(timeLeft)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                settingsManager.timeLeft = timeLeft
            }
        })
    }
    
    private fun registerSubmitReceiver() {
        if (receiverRegistered)
            return

        receiverRegistered = true
        
        val lbm = LocalBroadcastManager.getInstance(activity!!)
        val intentFilter = IntentFilter(SubmitService.POST_SUBMITTED)
        lbm.registerReceiver(submitReceiver, intentFilter)
    }
    
    private fun unregisterSubmitReceiver() {
        if (!receiverRegistered)
            return
        
        receiverRegistered = false
        
        val lbm = LocalBroadcastManager.getInstance(activity!!)
        lbm.unregisterReceiver(submitReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_queue, container, false)

        recyclerView = v.findViewById(R.id.queue_recyclerview)
        timerToggle = v.findViewById(R.id.queue_toggle)
        seekBar = v.findViewById(R.id.queue_seekbar)
        timerText = v.findViewById(R.id.queue_timer)

        initUi()
        
        return v
    }
    
    @SuppressLint("SetTextI18n") // todo: deal with this
    private fun updateToggleViews(autosubmitEnabled: Boolean) {
        
        if (autosubmitEnabled) {
            timerToggle.text = "Turn off"
            seekBar.isEnabled = false
        } else {
            timerToggle.text = "Turn on"
            seekBar.isEnabled = true
        }

        updateTimerViews(settingsManager.timeLeft!!)
    }
    
    private fun updateTimerViews(timeLeft: Duration) {
        updateSeekbarProgress(timeLeft)
        updateTimerText(timeLeft)
    }

    private fun toggleAutosubmit(on: Boolean) {
        
        if (on == settingsManager.autosubmitEnabled) // this should never happen
            throw RuntimeException("Can't change autosubmit to state it's already in")
        
        if (on) {
            if (!reddit.isLoggedIn) {
                toast("You must be signed in to autosubmit")
                return
            }

            if (queue.posts.isEmpty()) {
                toast("No posts to autosubmit")
                return
            }

            settingsManager.autosubmitEnabled = true
            registerSubmitReceiver()
            
            timerObject = getTimerObject(settingsManager.timeLeft!!)
            timerObject.start()
            
            postScheduler.schedulePeriodicPosts(queue.posts, settingsManager.period, settingsManager.timeLeft!!)
            
            log("Scheduled ${queue.posts.size} post(s)")
            
        } else {
            settingsManager.autosubmitEnabled = false
            unregisterSubmitReceiver()
            
            timerObject.cancel()
            settingsManager.timeLeft = Duration(DateTime.now(), queue.posts.first().scheduledDate!!)
            
            postScheduler.cancelScheduledPosts(queue.posts.reversed()) // ...its for optimization

            log("Canceled ${queue.posts.size} scheduled post(s)")
        }

        updateToggleViews(on)
    }
    
    private fun getTimerObject(timeLeft: Duration): CountDownTimer {
        return object : CountDownTimer(timeLeft.millis, TIMER_UPDATE_INTERVAL_MS) {
            
            override fun onFinish() {
                // todo: remove the submitted item from post list
            }

            override fun onTick(millisUntilFinished: Long) {
                val timeUntilFinished = Duration(millisUntilFinished)
                updateTimerViews(timeUntilFinished)
            }
        }
    }
    
    private fun updateSeekbarProgress(timeLeft: Duration) {
        val millis = timeLeft.millis
        val percentage = 1 - millis.toFloat() / settingsManager.period.millis
        val rounded = Math.round(percentage * SEEKBAR_MAX_VALUE)
        seekBar.progress = rounded
    }
    
    private fun updateTimerText(timeLeft: Duration) {
        timerText.text = timeLeft.toNice()
    }

    override fun onStart() {
        super.onStart()

        // assume period and autosubmit type never change while autosubmit is enabled
        // todo: actually prohibit changing these values while autosubmit is on

        if (settingsManager.timeLeft == null)
            settingsManager.timeLeft = settingsManager.period
        
        if (settingsManager.autosubmitEnabled) {
            handleEnabledAutosubmit()
        } else {
            if (settingsManager.timeLeft!! > settingsManager.period)
                settingsManager.timeLeft = settingsManager.period
        }

        updateToggleViews(settingsManager.autosubmitEnabled)
        updatePostList()
        
        val msg = when (settingsManager.autosubmitType) {
            SettingsManager.AutosubmitType.Manual -> "MANUAL"
            SettingsManager.AutosubmitType.Periodic -> "PERIODIC"
        }

        toast(msg)
    }

    override fun onStop() {
        super.onStop()

        if (settingsManager.autosubmitEnabled)
            timerObject.cancel()

        unregisterSubmitReceiver()
    }

    private fun updatePostList() {
        adapter.setPosts(queue.posts)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_queue, menu)

        val redditLoginMenuItem = menu!!.findItem(R.id.menu_queue_reddit_login)!!
        val imgurLoginMenuItem = menu.findItem(R.id.menu_queue_imgur_login)!!
        
        redditLoginMenuItem.isEnabled = !redditTokenFetching && !reddit.isLoggedIn
        imgurLoginMenuItem.isEnabled = !imgurAccount.isLoggedIn
    }

    private fun createNewPost() {
        val i = NewPostActivity.newIntent(context!!, allowScheduledDateEditing)
        startActivityForResult(i, REQUEST_CODE_NEW_POST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK)
            return
        
        if (requestCode == REQUEST_CODE_NEW_POST) {
            val newPost = PostFragment.getNewPostFromResult(data!!)!!

            queue.addPost(newPost)
            
            if (settingsManager.autosubmitEnabled)
                postScheduler.scheduleUnscheduledPostsPeriodic(settingsManager.period)
        }
    }

    private fun showRedditLoginDialog() {
        
        val authDialog = Dialog(context!!)
        authDialog.setContentView(R.layout.dialog_auth)
        
        authDialog.setOnCancelListener { toast("Fail") }

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)
        authWebview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (reddit.tryExtractCode(url)) {
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
    
    private fun openSettings() {
        val i = SettingsActivity.newIntent(context!!)
        startActivity(i)
    }
    
    private fun testButton() {
        toast(randomState())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.menu_queue_add -> {
                createNewPost()
                true
            }
            R.id.menu_queue_test -> {
                testButton()
                true
            }
            R.id.menu_queue_reddit_login -> {
                showRedditLoginDialog()
                true
            }
            R.id.menu_queue_imgur_login -> {
                showImgurLoginDialog()
                true
            }
            R.id.menu_queue_settings -> {
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showImgurLoginDialog() {

        val authDialog = Dialog(context!!)
        authDialog.setContentView(R.layout.dialog_auth)
        
        authDialog.setOnDismissListener {
            activity!!.invalidateOptionsMenu()
            toast(if (imgurAccount.isLoggedIn) "Success" else "Fail")
        }

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)
        authWebview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (imgurAccount.tryExtractTokens(url))
                    authDialog.dismiss()
            }
        }

        authWebview.loadUrl(imgurAccount.authorizationUrl)
        authDialog.show()
    }

    private inner class PostAdapter(private var posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostHolder>() {

        private inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleTextView: TextView
            private val contentTextView: TextView
            private val typeCheckBox: CheckBox

            private lateinit var post: Post

            init {
                itemView.setOnClickListener({
                    val i = PostPagerActivity.newIntent(context!!, post.id, allowScheduledDateEditing)
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
            val post = posts[position]
            holder.bindPost(post)
        }

        fun setPosts(posts_: List<Post>) {
            posts = posts_
            notifyDataSetChanged()
        }

        override fun getItemCount() = posts.size
    }
    
    companion object {
        private const val TIMER_UPDATE_INTERVAL_MS: Long = 100 // 0.1 seconds
        private const val SEEKBAR_MAX_VALUE = 1000
        private const val REQUEST_CODE_NEW_POST = 0
    }
}
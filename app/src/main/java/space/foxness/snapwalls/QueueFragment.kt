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
import com.github.debop.kodatimes.times
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import org.joda.time.Duration
import space.foxness.snapwalls.Util.log
import space.foxness.snapwalls.Util.toNice
import space.foxness.snapwalls.Util.toast
import java.util.*

class QueueFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private lateinit var signinMenuItem: MenuItem
    private lateinit var timerText: TextView
    private lateinit var timerToggle: Button
    private lateinit var seekBar: SeekBar
    
    private lateinit var timerObject: CountDownTimer

    private lateinit var period: Duration
    
    private lateinit var config: Config
    private lateinit var queue: Queue
    private lateinit var reddit: Reddit
    private lateinit var postScheduler: PostScheduler
    
    private var receiverRegistered = false
    
    private val submitReceiver = object: BroadcastReceiver() {
        
        override fun onReceive(context: Context?, intent: Intent?) {
            
            toast("post submitted :O")
            handleEnabledAutosubmit()
            updatePostList()
        }
    }
    
    private fun handleEnabledAutosubmit() {
        
        if (queue.posts.isEmpty()) {
            config.autosubmitEnabled = false
            config.timeLeft = period
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
        config = Config.getInstance(ctx)
        queue = Queue.getInstance(ctx)
        reddit = Autoreddit.getInstance(ctx).reddit
        postScheduler = PostScheduler(ctx)

        PreferenceManager.setDefaultValues(ctx, R.xml.preferences, false)

        if (config.timeLeft == null)
            config.timeLeft = period
    }
    
    private fun retrievePeriod(): Duration {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        val minutes = preferences.getInt(SettingsFragment.PREF_PERIOD_MINUTES, -1)
        
        if (minutes == -1)
            throw Exception("Default value wasn't set")
        
        val millisInMinute: Long = 60 * 1000
        return Duration(minutes * millisInMinute)
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
            toggleAutosubmit(!config.autosubmitEnabled)
            button.isEnabled = true
        }

        // SEEKBAR ------------------------------------

        seekBar.max = SEEKBAR_MAX_VALUE
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            private var timeLeft: Duration = Duration.ZERO

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val percentage = 1 - progress.toDouble() / SEEKBAR_MAX_VALUE
                    val millis = period.millis * percentage
                    val rounded = Math.round(millis)
                    timeLeft = Duration(rounded)
                    updateTimerText(timeLeft)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                config.timeLeft = timeLeft
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

        updateTimerViews(config.timeLeft!!)
    }
    
    private fun updateTimerViews(timeLeft: Duration) {
        updateSeekbarProgress(timeLeft)
        updateTimerText(timeLeft)
    }

    private fun toggleAutosubmit(on: Boolean) {
        
        if (on == config.autosubmitEnabled) // this should never happen
            throw RuntimeException("Can't change autosubmit to state it's already in")
        
        if (on) {
            if (!reddit.isSignedIn) {
                toast("You must be signed in to autosubmit")
                return
            }

            if (queue.posts.isEmpty()) {
                toast("No posts to autosubmit")
                return
            }
            
            val postDelays = HashMap(queue.posts
                    .mapIndexed { i, post -> post.id to config.timeLeft!! + period * i.toLong() }
                    .toMap())
            
            timerObject = getTimerObject(config.timeLeft!!)
            timerObject.start()
            
            postScheduler.scheduleDelayedPosts(postDelays)

            val now = DateTime.now()
            queue.posts.forEach {
                it.scheduledDate = now + postDelays[it.id]
                queue.updatePost(it)
            }

            config.autosubmitEnabled = true
            registerSubmitReceiver()
            
            log("Scheduled ${queue.posts.size} post(s)")
            
        } else {
            timerObject.cancel()
            config.timeLeft = Duration(DateTime.now(), queue.posts.first().scheduledDate!!)
            
            postScheduler.cancelScheduledPosts(queue.posts.map { it.id })

            queue.posts.forEach {
                it.scheduledDate = null
                queue.updatePost(it)
            }

            config.autosubmitEnabled = false
            unregisterSubmitReceiver()

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
        val percentage = 1 - millis.toFloat() / period.millis
        val rounded = Math.round(percentage * SEEKBAR_MAX_VALUE)
        seekBar.progress = rounded
    }
    
    private fun updateTimerText(timeLeft: Duration) {
        timerText.text = timeLeft.toNice()
    }

    override fun onStart() {
        super.onStart()

        // assume period never changes while autosubmit is enabled
        // todo: actually prohibit period changing while autosubmit is on
        
        period = retrievePeriod()
        
        if (config.autosubmitEnabled) {
            handleEnabledAutosubmit()
        } else {
            if (config.timeLeft!! > period)
                config.timeLeft = period
        }

        updateToggleViews(config.autosubmitEnabled)
        updatePostList()
    }

    override fun onStop() {
        super.onStop()

        if (config.autosubmitEnabled)
            timerObject.cancel()

        unregisterSubmitReceiver()
    }

    private fun updatePostList() {
        adapter.setPosts(queue.posts)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_queue, menu)

        signinMenuItem = menu!!.findItem(R.id.menu_queue_signin)

        updateMenu()
    }

    private fun createNewPost() {
        val i = NewPostActivity.newIntent(context!!)
        startActivityForResult(i, REQUEST_CODE_NEW_POST)
    }
    
    // this method expects the queue to be divided into 2 segments
    // the first segment is the scheduled segment at the beginning
    // the last segment is the segment that will be scheduled
    private fun scheduleAllUnscheduledPostsPeriodic() {
        val posts = queue.posts
        
        if (posts.size < 2)
            throw Exception("Need at least 2 posts")
        
        if (posts.first().scheduledDate == null)
            throw Exception("Can't infer the periodic schedule")
        
        var onlyNullsNow = false
        for (i in 1 until posts.size) {
            if (posts[i].scheduledDate == null) {
                posts[i].scheduledDate = posts[i - 1].scheduledDate!! + period
                queue.updatePost(posts[i])
                onlyNullsNow = true
            } else if (onlyNullsNow) {
                throw Exception("You can't switch from nulls to non-nulls")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK)
            return
        
        if (requestCode == REQUEST_CODE_NEW_POST) {
            val newPost = PostFragment.getNewPostFromResult(data!!)!!

            queue.addPost(newPost)
            
            if (config.autosubmitEnabled)
                scheduleAllUnscheduledPostsPeriodic()
        }
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
                        reddit.fetchAuthTokens()

                        uiThread {
                            toast(if (reddit.canSubmitRightNow) "Success" else "Fail")
                            updateMenu()
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.menu_queue_add -> {
                createNewPost()
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
            R.id.menu_queue_settings -> {
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun submitTopPost() { // todo: remove

        toast("This button is deprecated :P")
        
//        if (queue.posts.isEmpty()) {
//            toast("No post to submit")
//            return
//        }
//
//        if (!reddit.canSubmitRightNow) {
//            toast("Can't submit right now")
//            return
//        }
//        
//        reddit.submit(queue.posts.first(), { error, link ->
//            if (error != null)
//                throw error
//
//            toast("GOT LINK: $link")
//        })
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
package me.nocturnl.inksnap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.text.InputType
import android.text.format.DateUtils
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.*
import org.joda.time.DateTime
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.toast
import me.nocturnl.inksnap.Util.latestPostDate

abstract class QueueFragment : Fragment()
{
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var timerText: TextView
    protected lateinit var timerToggle: Button
    protected lateinit var adapter: PostAdapter
    protected lateinit var timerObject: CountDownTimer

    protected lateinit var settingsManager: SettingsManager
    protected lateinit var queue: Queue
    protected lateinit var reddit: Reddit
    protected lateinit var postScheduler: PostScheduler
    protected lateinit var thumbnailCache: ThumbnailCache
    
    protected var toggleRestrictorJob: Job? = null
    
    protected var searchQuery: String? = null

    private var receiverRegistered = false
    
    protected abstract val allowIntendedSubmitDateEditing: Boolean
    
    protected abstract val fragmentLayoutId: Int
    
    protected abstract fun toggleSubmission(on: Boolean)
    
    protected lateinit var thumbnailDownloader: ThumbnailDownloader<PostHolder>

    protected open fun onSubmissionServiceDoneReceived(context: Context, intent: Intent) { }

    protected open fun onImport() { }
    
    protected open fun onInitUi(v: View)
    {
        recyclerView = v.findViewById(R.id.queue_recyclerview)
        timerToggle = v.findViewById(R.id.queue_toggle)
        timerText = v.findViewById(R.id.queue_timer)

        // RECYCLER VIEW ------------------------------

        recyclerView.layoutManager = LinearLayoutManager(context!!)

        adapter = PostAdapter(queue.posts) // todo: refactor to not have args
        recyclerView.adapter = adapter

        // TIMER TOGGLE -------------------------------

        timerToggle.setOnClickListener { button ->
            toggleSubmission(!settingsManager.submissionEnabled)
        }
    }
    
    protected open fun onTimerFinish() { }
    
    protected open fun onTimerTick(millisUntilFinished: Long) { }

    private val submissionServiceDoneReceiver: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            onSubmissionServiceDoneReceived(context, intent)
        }
    }

    protected open fun onNewPostAdded(newPost: Post) { }

    protected open fun onPostEdited(editedPost: Post)
    {
        val postBeforeChangeId = editedPost.id
        
        val runOnEnabledSubmission = { postBeforeChange: Post ->
            
            // because editedPost's scheduled is outdated
            // outdated because it was set when postholder binded the post to itself
            editedPost.scheduled = postBeforeChange.scheduled
            // -----------------------------------------------------------
            
            val shouldBeRescheduled = !editedPost.intendedSubmitDate!!.isEqual(postBeforeChange.intendedSubmitDate)

            if (shouldBeRescheduled)
            {
                postScheduler.cancelScheduledPost(postBeforeChange)
                editedPost.scheduled = false
            }

            queue.updatePost(editedPost)

            if (shouldBeRescheduled)
            {
                postScheduler.schedulePost(editedPost)
            }
        }
        
        val runOnDisabledSubmission = {
            queue.updatePost(editedPost)
        }
        
        postChangeSafeguard(postBeforeChangeId, runOnEnabledSubmission, runOnDisabledSubmission)
    }

    protected open fun onPostDeleted(deletedPostId: String) { }
    
    protected fun postChangeSafeguard(
            postBeforeChangeId: String,
            runOnEnabledSubmission: (postBeforeChange: Post) -> Unit,
            runOnDisabledSubmission: () -> Unit)
    {
        val postBeforeChange = queue.getPost(postBeforeChangeId)
        
        if (postBeforeChange == null)
        {
            // todo: long toast or snackbar
            toast("Whoops, the post you tried to edit or delete has already been posted :)")
        }
        else
        {
            if (settingsManager.submissionEnabled)
            {
                val editThresholdDate = postBeforeChange.intendedSubmitDate!! - Duration(POST_EDIT_THRESHOLD_MS)
                if (DateTime.now() > editThresholdDate)
                {
                    // todo: long toast or snackbar
                    toast("Can't edit or delete posts within 3 seconds of them being posted :P")
                }
                else
                {
                    runOnEnabledSubmission(postBeforeChange)
                }
            }
            else
            {
                runOnDisabledSubmission()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // todo: to be more efficient don't write to config on every change
        // write like in onPause or something
        // im talking mostly about config.timeLeft and config.submissionEnabled

        val ctx = context!!
        settingsManager = SettingsManager.getInstance(ctx)
        queue = Queue.getInstance(ctx)
        postScheduler = PostScheduler.getInstance(ctx)
        reddit = Autoreddit.getInstance(ctx).reddit
        thumbnailCache = ThumbnailCache.getInstance(ctx)
        
        val responseHandler = Handler()
        thumbnailDownloader = ThumbnailDownloader(responseHandler)
        
        val tdl = object : ThumbnailDownloader.ThumbnailDownloadListener<PostHolder>
        {
            override fun onThumbnailDownloaded(target: PostHolder, thumbnail: Bitmap)
            {
                if (thumbnailCache.contains(target.post.getThumbnailId()))
                {
                    throw Exception("How did this even happen?")
                    // todo: have each thumbnail download request contain the post id
                    // to compare it here
                    
                    // todo: also add url that corresponds to the thumbnail
                }
                
                thumbnailCache.add(target.post.getThumbnailId(), thumbnail)
                target.setThumbnail(thumbnail)
            }
        }
        
        thumbnailDownloader.setThumbnailDownloadListener(tdl)
        thumbnailDownloader.start()
        thumbnailDownloader.getLooper()

        // todo: handle posts that should have been posted but weren't
    }

    override fun onDestroy()
    {
        super.onDestroy()
        thumbnailDownloader.quit()
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        thumbnailDownloader.clearQueue()
    }

    final override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(fragmentLayoutId, container, false)
        
        onInitUi(v)
        
        return v
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_queue, menu)

        val developerOptionsUnlocked = settingsManager.developerOptionsUnlocked

        val checkServiceItem = menu.findItem(R.id.menu_queue_check_service)
        val importItem = menu.findItem(R.id.menu_queue_import)
        val exportItem = menu.findItem(R.id.menu_queue_export)

        checkServiceItem.isVisible = developerOptionsUnlocked
        importItem.isVisible = developerOptionsUnlocked
        exportItem.isVisible = developerOptionsUnlocked

        val searchItem = menu.findItem(R.id.menu_queue_search)
        val searchView = searchItem.actionView as SearchView
        
        val qtl = object : SearchView.OnQueryTextListener
        {
            override fun onQueryTextSubmit(query: String): Boolean
            {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean
            {
                val query = if (newText.isEmpty())
                {
                    null
                }
                else
                {
                    newText
                }
                
                searchQuery = query
                updatePostList()
                return true
            }
        }

        val ael = object : MenuItem.OnActionExpandListener
        {
            override fun onMenuItemActionExpand(item: MenuItem?) = true

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean
            {
                searchQuery = null
                updatePostList()
                return true
            }
        }
        
        searchView.setOnQueryTextListener(qtl)
        searchItem.setOnActionExpandListener(ael)
    }

    protected fun updatePostList()
    {
        var posts = queue.posts
        
        val stringQuery = searchQuery
        if (stringQuery != null)
        {
            var regexQuery: Regex? = null
            
            try
            {
                regexQuery = stringQuery.toRegex(RegexOption.IGNORE_CASE)
            }
            catch (exception: Exception)
            {
                // nuthin
            }

            val predicate = if (regexQuery == null)
            {
                { it: Post -> it.title.contains(stringQuery) 
                              || it.content.contains(stringQuery) 
                              || it.subreddit.contains(stringQuery) }
            }
            else
            {
                { it: Post -> it.title.contains(regexQuery) 
                              || it.content.contains(regexQuery) 
                              || it.subreddit.contains(regexQuery) }
            }
            
            posts = posts.filter(predicate)
        }
        
        val comparator = when (settingsManager.sortBy)
        {
            SettingsManager.SortBy.Title -> Util.titlePostComparator
            SettingsManager.SortBy.Date -> Util.datePostComparator
        }

        val sortedPosts = posts.sortedWith(comparator)
        adapter.setPosts(sortedPosts)
    }

    protected fun createNewPost()
    {
        val i = PostActivity.newIntent(context!!, null, allowIntendedSubmitDateEditing)
        startActivityForResult(i, REQUEST_CODE_NEW_POST)
    }
    
    protected fun sortByTitle()
    {
        settingsManager.sortBy = SettingsManager.SortBy.Title
        updatePostList()
    }
    
    protected fun sortByDate()
    {
        settingsManager.sortBy = SettingsManager.SortBy.Date
        updatePostList()
    }

    protected fun checkService()
    {
        val s = postScheduler.isServiceScheduled().toString()
        toast(s)
    }

    protected open fun clear() { }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            R.id.menu_queue_add -> { createNewPost(); true }
            R.id.menu_queue_import -> { importPosts(); true }
            R.id.menu_queue_export -> { exportPosts(); true }
            R.id.menu_queue_sort_by_title -> { sortByTitle(); true }
            R.id.menu_queue_sort_by_date -> { sortByDate(); true }
            R.id.menu_queue_clear -> { clear(); true }
            R.id.menu_queue_check_service -> { checkService(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    protected fun importPosts()
    {
        val ctx = context!!

        val input = EditText(ctx) // todo: switch to layout
        input.setLines(5)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        
        val onOk = { di: DialogInterface, which: Int -> Unit
            
            val posts = input.text.toString()
                    .trim()
                    .replace("\r", "")
                    .split("\n\n")
            
            val now = DateTime.now()
            val startingDate = queue.posts.latestPostDate()?.plusDays(1) ?: now
            val generatedDates = Util.generatePostDates(startingDate, now, posts.size)
            
            posts.asSequence().map { it.split("\n") }
                    .mapIndexed { index, list ->
                        val post = Post.newInstance()
                        post.title = list[0].trim()
                        post.content = list[1].trim()
                        post.isLink = true
                        post.subreddit = "wallpapers"
                        post.intendedSubmitDate = generatedDates[index]
                        post
                    }.toList()
                    .forEach { 
                        onNewPostAdded(it)
                    }

            onImport()
        }

        val inputDialog = AlertDialog.Builder(ctx)
                .setView(input)
                .setTitle("Posts") // todo: extracteroni
                .setPositiveButton(android.R.string.ok, onOk)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

        inputDialog.show()
    }
    
    protected fun exportPosts()
    {
        val result = queue.posts.sortedBy { it.intendedSubmitDate }.joinToString(separator = "") { "${it.title}\n${it.content}\n\n" }.trimEnd()
        val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip = ClipData.newPlainText("Posts", result)
        toast("Copied")
    }

    protected fun startTimer(timeLeft: Duration)
    {
        fun getTimerObject(timeLeft_: Duration): CountDownTimer
        {
            return object : CountDownTimer(timeLeft_.millis, TIMER_UPDATE_INTERVAL_MS)
            {
                override fun onFinish()
                {
                    onTimerFinish()
                }

                override fun onTick(millisUntilFinished: Long)
                {
                    onTimerTick(millisUntilFinished)
                }
            }
        }

        timerObject = getTimerObject(timeLeft)
        timerObject.start()
    }

    protected fun startTimerAndRegisterReceiver(timeLeft: Duration)
    {
        startTimer(timeLeft)
        registerSubmissionServiceDoneReceiver()
    }

    protected fun registerSubmissionServiceDoneReceiver()
    {
        if (receiverRegistered)
        {
            return
        }

        receiverRegistered = true

        val lbm = LocalBroadcastManager.getInstance(activity!!)
        val intentFilter = SubmissionService.getSubmissionServiceDoneIntentFilter()
        lbm.registerReceiver(submissionServiceDoneReceiver, intentFilter)
    }

    protected fun unregisterSubmissionServiceDoneReceiver()
    {
        if (!receiverRegistered)
        {
            return
        }

        receiverRegistered = false

        val lbm = LocalBroadcastManager.getInstance(activity!!)
        lbm.unregisterReceiver(submissionServiceDoneReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when (requestCode)
        {
            REQUEST_CODE_NEW_POST ->
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    val newPost = PostActivity.getPostFromResult(data!!)
                    onNewPostAdded(newPost)
                }
            }

            REQUEST_CODE_EDIT_POST ->
            {
                when (resultCode)
                {
                    Activity.RESULT_OK -> // ok means the post was saved
                    {
                        val editedPost = PostActivity.getPostFromResult(data!!)
                        onPostEdited(editedPost)
                    }

                    PostActivity.RESULT_CODE_DELETED ->
                    {
                        val deletedPostId = PostActivity.getDeletedPostIdFromResult(data!!)
                        onPostDeleted(deletedPostId)
                    }
                }
            }
            
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    protected inner class PostAdapter(private var posts: List<Post>) : RecyclerView.Adapter<PostHolder>()
    {
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
            
            if (post.isLink)
            {
                val cachedThumbnail = thumbnailCache.get(post.getThumbnailId())
                if (cachedThumbnail == null)
                {
                    GlobalScope.launch {
                        val thumbnailUrlToBeQueued = ServiceProcessor.tryGetThumbnailOrDirectUrl(post.content)

                        if (thumbnailUrlToBeQueued == null)
                        {
                            thumbnailDownloader.unqueueThumbnail(holder)
                        }
                        else
                        {
                            thumbnailDownloader.queueThumbnail(holder, thumbnailUrlToBeQueued)
                        }
                    }
                }
                else
                {
                    thumbnailDownloader.unqueueThumbnail(holder)
                    holder.setThumbnail(cachedThumbnail)
                }
            }
            else
            {
                thumbnailDownloader.unqueueThumbnail(holder)
            }
        }

        fun setPosts(posts_: List<Post>)
        {
            posts = posts_
            notifyDataSetChanged()
            onPostsChanged(posts_.size)
        }

        override fun getItemCount() = posts.size
    }
    
    protected open fun onPostsChanged(count: Int) { }

    protected inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleView: TextView
        private val contentView: TextView
        private val thumbnailView: ImageView
        private val datetimeView: TextView
        private val subredditView: TextView

        lateinit var post: Post
            private set

        init
        {
            itemView.setOnClickListener {
                val i = PostActivity.newIntent(context!!, post, allowIntendedSubmitDateEditing)
                startActivityForResult(i, REQUEST_CODE_EDIT_POST)
            }

            titleView = itemView.findViewById(R.id.queue_post_title)
            contentView = itemView.findViewById(R.id.queue_post_content)
            thumbnailView = itemView.findViewById(R.id.queue_post_thumbnail)
            datetimeView = itemView.findViewById(R.id.queue_post_datetime)
            subredditView = itemView.findViewById(R.id.queue_post_subreddit)
        }

        @SuppressLint("SetTextI18n") // todo: fix
        fun bindPost(p: Post)
        {
            post = p
            titleView.text = post.title
            
            contentView.visibility = Util.getVisibilityGoneConstant(post.content.isNotBlank())
            contentView.text = post.content
            
            subredditView.text = "/r/" + post.subreddit
            
            val postIsd = post.intendedSubmitDate
            val relativeDateString = if (postIsd == null)
            {
                "unknown"
            }
            else
            {
                DateUtils.getRelativeTimeSpanString(postIsd.millis, System.currentTimeMillis(), 0)
            }
            
            datetimeView.text = relativeDateString
            
            val thumbId = if (post.isLink) R.drawable.thumbnail_link_post else R.drawable.thumbnail_text_post
            val thumbnail = resources.getDrawable(thumbId, context?.theme)
            setThumbnail(thumbnail)
        }
        
        // warning: thumbnail must be a square, not a rectangle
        fun setThumbnail(thumbnail: Bitmap)
        {
            val drawable = RoundedBitmapDrawableFactory.create(resources, thumbnail)
            drawable.cornerRadius = Math.max(thumbnail.width, thumbnail.height) / 2f
            setThumbnail(drawable)
        }
        
        private fun setThumbnail(thumbnail: Drawable)
        {
            thumbnailView.setImageDrawable(thumbnail)
        }
    }

    protected fun startToggleRestrictorJob(timeLeftUntilPost: Duration)
    {
        val timeLeftUntilCantToggleSubmission = timeLeftUntilPost - Duration(SUBMISSION_TOGGLE_THRESHOLD_MS)
        if (timeLeftUntilCantToggleSubmission > Duration.ZERO)
        {
            toggleRestrictorJob =
                    GlobalScope.launch {
                        delay(timeLeftUntilCantToggleSubmission.millis)
        
                        if (isActive)
                        {
                            restrictTimerToggle()
                        }
                    }
        }
        else
        {
            restrictTimerToggle()
        }
    }

    protected fun stopToggleRestrictorJob()
    {
        toggleRestrictorJob?.cancel()
        toggleRestrictorJob = null
    }

    protected fun restrictTimerToggle()
    {
        timerToggle.isEnabled = false
    }

    protected fun unrestrictTimerToggle()
    {
        timerToggle.isEnabled = true
    }
    
    companion object
    {
        // protected static not supported yet
        
        const val POST_EDIT_THRESHOLD_MS: Long = 3000
        
        const val SUBMISSION_TOGGLE_THRESHOLD_MS: Long = 3000
        
        private const val REQUEST_CODE_NEW_POST = 0
        private const val REQUEST_CODE_EDIT_POST = 1
        private const val TIMER_UPDATE_INTERVAL_MS: Long = 1000
    }
}
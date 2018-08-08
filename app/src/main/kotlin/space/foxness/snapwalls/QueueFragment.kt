package space.foxness.snapwalls

import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import org.joda.time.Duration
import space.foxness.snapwalls.Util.toast

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
    
    protected var searchQuery: String? = null

    private var receiverRegistered = false
    
    protected abstract val allowIntendedSubmitDateEditing: Boolean
    
    protected abstract val fragmentLayoutId: Int
    
    protected abstract fun toggleAutosubmit(on: Boolean)
    
    protected abstract fun onNewPostAdded(newPost: Post)
    
    protected abstract fun onPostEdited(editedPost: Post)
    
    protected abstract fun onPostDeleted(deletedPostId: String)
    
    protected lateinit var thumbnailDownloader: ThumbnailDownloader<PostHolder>

    protected open fun onAutosubmitServiceDoneReceived(context: Context, intent: Intent)
    {
        toast("service is done :O")
    }
    
    protected open fun onInitUi(v: View)
    {
        recyclerView = v.findViewById(R.id.queue_recyclerview)
        timerToggle = v.findViewById(R.id.queue_toggle)
        timerText = v.findViewById(R.id.queue_timer)

        // RECYCLER VIEW ------------------------------

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context,
                                                             DividerItemDecoration.VERTICAL))

        adapter = PostAdapter(queue.posts) // todo: refactor to not have args
        recyclerView.adapter = adapter

        // TIMER TOGGLE -------------------------------

        timerToggle.setOnClickListener { button ->
            button.isEnabled = false
            toggleAutosubmit(!settingsManager.autosubmitEnabled)
            button.isEnabled = true
        }
    }
    
    protected open fun onTimerFinish() { }
    
    protected open fun onTimerTick(millisUntilFinished: Long) { }

    private val autosubmitServiceDoneReceiver: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            onAutosubmitServiceDoneReceived(context, intent)
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
        thumbnailCache = ThumbnailCache.getInstance(ctx)
        
        val responseHandler = Handler()
        thumbnailDownloader = ThumbnailDownloader(responseHandler)
        
        val tdl = object : ThumbnailDownloader.ThumbnailDownloadListener<PostHolder>
        {
            override fun onThumbnailDownloaded(target: PostHolder, thumbnail: Bitmap)
            {
                if (thumbnailCache.contains(target.getPostId()))
                {
                    throw Exception("How did this even happen?")
                    // todo: have each thumbnail download request contain the post id
                    // to compare it here
                    
                    // todo: also add url that corresponds to the thumbnail
                }
                
                thumbnailCache.add(target.getPostId(), thumbnail)
                target.setThumbnail(thumbnail)

                toast("used downloaded thumbnail")
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            R.id.menu_queue_add -> { createNewPost(); true }
            R.id.menu_queue_extract -> { extractPosts(); true }
            R.id.menu_queue_sort_by_title -> { sortByTitle(); true }
            R.id.menu_queue_sort_by_date -> { sortByDate(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    protected fun extractPosts()
    {
        val result = queue.posts.joinToString(separator = "") { "${it.title}\n${it.content}\n\n" }.trimEnd()
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
        registerAutosubmitServiceDoneReceiver()
    }

    protected fun registerAutosubmitServiceDoneReceiver()
    {
        if (receiverRegistered)
        {
            return
        }

        receiverRegistered = true

        val lbm = LocalBroadcastManager.getInstance(activity!!)
        val intentFilter = AutosubmitService.getAutosubmitServiceDoneBroadcastIntentFilter()
        lbm.registerReceiver(autosubmitServiceDoneReceiver, intentFilter)
    }

    protected fun unregisterAutosubmitServiceDoneReceiver()
    {
        if (!receiverRegistered)
        {
            return
        }

        receiverRegistered = false

        val lbm = LocalBroadcastManager.getInstance(activity!!)
        lbm.unregisterReceiver(autosubmitServiceDoneReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when (requestCode)
        {
            REQUEST_CODE_NEW_POST ->
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    val newPost = PostFragment.getPostFromResult(data!!)
                    onNewPostAdded(newPost)
                }
            }

            REQUEST_CODE_EDIT_POST ->
            {
                when (resultCode)
                {
                    Activity.RESULT_OK -> // ok means the post was saved
                    {
                        val editedPost = PostFragment.getPostFromResult(data!!)
                        onPostEdited(editedPost)
                    }

                    PostFragment.RESULT_CODE_DELETED ->
                    {
                        val deletedPostId = PostFragment.getDeletedPostIdFromResult(data!!)
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
            
            var thumbnailUrl: String? = null
            if (post.isLink)
            {
                val cachedThumbnail = thumbnailCache.get(post.id)
                if (cachedThumbnail == null)
                {
                    thumbnailUrl = ServiceProcessor.tryGetThumbnailUrl(post.content)
                }
                else
                {
                    holder.setThumbnail(cachedThumbnail)
                }
            }
            
            if (thumbnailUrl == null)
            {
                thumbnailDownloader.unqueueThumbnail(holder)
            }
            else
            {
                thumbnailDownloader.queueThumbnail(holder, thumbnailUrl)
            }
        }

        fun setPosts(posts_: List<Post>)
        {
            posts = posts_
            notifyDataSetChanged()
        }

        override fun getItemCount() = posts.size
    }

    protected inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleView: TextView
        private val contentView: TextView
        private val thumbnailView: ImageView

        private lateinit var post: Post

        init
        {
            itemView.setOnClickListener {
                val i = PostActivity.newIntent(context!!, post, allowIntendedSubmitDateEditing)
                startActivityForResult(i, REQUEST_CODE_EDIT_POST)
            }

            titleView = itemView.findViewById(R.id.queue_post_title)
            contentView = itemView.findViewById(R.id.queue_post_content)
            thumbnailView = itemView.findViewById(R.id.queue_post_thumbnail)
        }

        fun bindPost(p: Post)
        {
            post = p
            titleView.text = post.title
            contentView.text = post.content
            
            val thumbId = if (post.isLink) R.drawable.link_thumb else R.drawable.self_thumb
            val thumbnail = resources.getDrawable(thumbId, context?.theme)
            setThumbnail(thumbnail)
        }
        
        fun getPostId() = post.id
        
        fun setThumbnail(thumbnail: Bitmap)
        {
            setThumbnail(BitmapDrawable(resources, thumbnail))
        }
        
        fun setThumbnail(thumbnail: Drawable)
        {
            thumbnailView.setImageDrawable(thumbnail)
        }
    }

    // protected vals in companion are not yet supported
    
    protected val REQUEST_CODE_NEW_POST = 0
    protected val REQUEST_CODE_EDIT_POST = 1
    protected val TIMER_UPDATE_INTERVAL_MS: Long = 100 // 0.1 seconds
}
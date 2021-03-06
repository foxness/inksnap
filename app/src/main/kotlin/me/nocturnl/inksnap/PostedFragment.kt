package me.nocturnl.inksnap

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class PostedFragment : Fragment()
{
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_posted, container, false)

        val postedPostRepository = PostedPostRepository.getInstance(context!!)
        
        // todo: add sorting options
        val postedPosts = postedPostRepository.postedPosts.sortedByDescending { it.intendedSubmitDate }
        
        val noPostedPosts = postedPosts.isEmpty()
        
        recyclerView = v.findViewById(R.id.posted_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.visibility = Util.getVisibilityGoneConstant(!noPostedPosts)
        
        val adapter = PostedPostAdapter(postedPosts) // todo: refactor to not have args?
        recyclerView.adapter = adapter
        
        emptyView = v.findViewById(R.id.posted_empty_view)
        emptyView.visibility = Util.getVisibilityGoneConstant(noPostedPosts)
        
        return v
    }
    
    private inner class PostedPostHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleView: TextView
        private val contentView: TextView
        private val subredditView: TextView
        private val datetimeView: TextView
        private val thumbnailView: ImageView
        
        private lateinit var postedPost: PostedPost
        
        init
        {
            itemView.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(postedPost.url))
                startActivity(i)
            }
            
            titleView = itemView.findViewById(R.id.posted_post_title)
            contentView = itemView.findViewById(R.id.posted_post_content)
            subredditView = itemView.findViewById(R.id.posted_post_subreddit)
            datetimeView = itemView.findViewById(R.id.posted_post_datetime)
            thumbnailView = itemView.findViewById(R.id.posted_post_thumbnail)
        }
        
        @SuppressLint("SetTextI18n") // todo: fix
        fun bindPostedPost(pp: PostedPost)
        {
            postedPost = pp
            titleView.text = postedPost.title

            contentView.visibility = Util.getVisibilityGoneConstant(postedPost.content.isNotBlank())
            contentView.text = postedPost.content
            
            subredditView.text = "/r/" + postedPost.subreddit

            val relativeDateString = DateUtils.getRelativeTimeSpanString(
                            postedPost.intendedSubmitDate.millis,
                            System.currentTimeMillis(),
                            0)

            datetimeView.text = relativeDateString

            val thumbId = if (postedPost.isLink) R.drawable.thumbnail_link_post else R.drawable.thumbnail_text_post
            val thumbnail = resources.getDrawable(thumbId, context?.theme)
            thumbnailView.setImageDrawable(thumbnail)
        }
    }
    
    private inner class PostedPostAdapter(private var postedPosts: List<PostedPost>) : RecyclerView.Adapter<PostedPostHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostedPostHolder
        {
            val inflater = LayoutInflater.from(context)
            val v = inflater.inflate(R.layout.posted_post, parent, false)
            return PostedPostHolder(v)
        }

        override fun getItemCount() = postedPosts.size

        override fun onBindViewHolder(holder: PostedPostHolder, position: Int)
        {
            val postedPost = postedPosts[position]
            holder.bindPostedPost(postedPost)
        }
    }

    companion object
    {
        fun newInstance() = PostedFragment()
    }
}
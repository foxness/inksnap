package space.foxness.snapwalls

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class PostedFragment : Fragment()
{
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_posted, container, false)
        
        recyclerView = v.findViewById(R.id.posted_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context,
                                                             DividerItemDecoration.VERTICAL))

        val postedPostRepository = PostedPostRepository.getInstance(context!!)
        val adapter = PostedPostAdapter(postedPostRepository.postedPosts) // todo: refactor to not have args?
        recyclerView.adapter = adapter
        
        return v
    }
    
    private inner class PostedPostHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleView: TextView
        private val contentView: TextView
        private val subredditView: TextView
        
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
        }
        
        fun bindPostedPost(pp: PostedPost)
        {
            postedPost = pp
            titleView.text = postedPost.title
            contentView.text = postedPost.content
            subredditView.text = postedPost.subreddit
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
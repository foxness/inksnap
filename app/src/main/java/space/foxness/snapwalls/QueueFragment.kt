package space.foxness.snapwalls

import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView

abstract class QueueFragment : Fragment()
{
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var timerText: TextView
    protected lateinit var timerToggle: Button

    protected lateinit var adapter: PostAdapter
    
    protected abstract val allowIntendedSubmitDateEditing: Boolean

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
        private val titleTextView: TextView
        private val contentTextView: TextView
        private val typeCheckBox: CheckBox

        private lateinit var post: Post

        init
        {
            itemView.setOnClickListener {
                val i = PostPagerActivity.newIntent(context!!, post.id, allowIntendedSubmitDateEditing)
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
}
package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class FailedFragment : Fragment()
{
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_failed, container, false)

        recyclerView = v.findViewById(R.id.failed_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context,
                                                             DividerItemDecoration.VERTICAL))

        val failedPostRepository = FailedPostRepository.getInstance(context!!)
        val adapter = FailedPostAdapter(failedPostRepository.failedPosts) // todo: refactor to not have args?
        recyclerView.adapter = adapter

        return v
    }

    private inner class FailedPostHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleView: TextView

        private lateinit var failedPost: FailedPost

        init
        {
//            itemView.setOnClickListener {
//                // todo: open?
//            }

            titleView = itemView.findViewById(R.id.failed_post_title)
        }

        fun bindFailedPost(fp: FailedPost)
        {
            failedPost = fp
            titleView.text = failedPost.failReason
        }
    }

    private inner class FailedPostAdapter(private var failedPosts: List<FailedPost>) : RecyclerView.Adapter<FailedPostHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FailedPostHolder
        {
            val inflater = LayoutInflater.from(context)
            val v = inflater.inflate(R.layout.failed_post, parent, false)
            return FailedPostHolder(v)
        }

        override fun getItemCount() = failedPosts.size

        override fun onBindViewHolder(holder: FailedPostHolder, position: Int)
        {
            val postedPost = failedPosts[position]
            holder.bindFailedPost(postedPost)
        }
    }
}
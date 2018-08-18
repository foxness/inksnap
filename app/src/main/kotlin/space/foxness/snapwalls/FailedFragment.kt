package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
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
        private val contentView: TextView
        private val subredditView: TextView
        private val failReasonView: TextView

        private lateinit var failedPost: FailedPost

        init
        {
            itemView.setOnClickListener {
                openDetailedReasonDialog()
            }
            
            titleView = itemView.findViewById(R.id.failed_post_title)
            contentView = itemView.findViewById(R.id.failed_post_content)
            subredditView = itemView.findViewById(R.id.failed_post_subreddit)
            failReasonView = itemView.findViewById(R.id.failed_post_fail_reason)
        }
        
        private fun openDetailedReasonDialog()
        {
            val dialog = AlertDialog.Builder(context!!)
                    .setTitle(failedPost.failReason)
                    .setMessage(failedPost.detailedReason)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()

            dialog.show()
        }

        @SuppressLint("SetTextI18n") // todo: fix
        fun bindFailedPost(fp: FailedPost)
        {
            failedPost = fp
            
            titleView.text = failedPost.title
            contentView.text = failedPost.content
            subredditView.text = "/r/${failedPost.subreddit}"
            failReasonView.text = failedPost.failReason
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

    companion object
    {
        fun newInstance() = FailedFragment()
    }
}
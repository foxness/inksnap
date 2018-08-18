package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class FailedFragment : Fragment()
{
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_failed, container, false)

        val failedPostRepository = FailedPostRepository.getInstance(context!!)
        val failedPosts = failedPostRepository.failedPosts
        val noFailedPosts = failedPosts.isEmpty()

        recyclerView = v.findViewById(R.id.failed_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.visibility = Util.getVisibilityGoneConstant(!noFailedPosts)
        
        val adapter = FailedPostAdapter(failedPosts) // todo: refactor to not have args?
        recyclerView.adapter = adapter
        
        emptyView = v.findViewById(R.id.failed_empty_view)
        emptyView.visibility = Util.getVisibilityGoneConstant(noFailedPosts)

        return v
    }

    private inner class FailedPostHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        private val titleView: TextView
        private val contentView: TextView
        private val subredditView: TextView
        private val failReasonView: TextView
        private val datetimeView: TextView
        private val thumbnailView: ImageView

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
            datetimeView = itemView.findViewById(R.id.failed_post_datetime)
            thumbnailView = itemView.findViewById(R.id.failed_post_thumbnail)
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

            contentView.visibility = Util.getVisibilityGoneConstant(failedPost.content.isNotBlank())
            contentView.text = failedPost.content
            
            subredditView.text = "/r/" + failedPost.subreddit
            failReasonView.text = "Fail reason: " + failedPost.failReason

            val relativeDateString = DateUtils.getRelativeTimeSpanString(
                    failedPost.intendedSubmitDate.millis,
                    System.currentTimeMillis(),
                    0)

            datetimeView.text = relativeDateString

            val thumbId = if (failedPost.isLink) R.drawable.link_thumb else R.drawable.self_thumb
            val thumbnail = resources.getDrawable(thumbId, context?.theme)
            thumbnailView.setImageDrawable(thumbnail)
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
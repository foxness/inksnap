package space.foxness.snapwalls

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity

class PostPagerActivity : AppCompatActivity()
{
    private lateinit var viewPager: ViewPager
    private lateinit var posts: List<Post>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_pager)

        val postId = intent.getIntExtra(EXTRA_POST_ID, -1)
        val allowIntendedSubmitDateEditing =
                intent.getBooleanExtra(EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING, false)

        viewPager = findViewById(R.id.activity_post_pager_viewpager)

        posts = Queue.getInstance(this).posts
        val fm = supportFragmentManager
        viewPager.adapter = object : FragmentStatePagerAdapter(fm)
        {
            override fun getItem(position: Int): Fragment
            {
                val s = posts[position]
                return PostFragment.newInstance(s.id, allowIntendedSubmitDateEditing)
            }

            override fun getCount() = posts.size
        }

        for (i in posts.indices)
        {
            if (posts[i].id == postId)
            {
                viewPager.currentItem = i
                break
            }
        }
    }

    companion object
    {
        private const val EXTRA_POST_ID = "post_id"
        private const val EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING =
                "allow_intended_submit_date_editing"

        fun newIntent(packageContext: Context,
                      postId: Int,
                      allowIntendedSubmitDateEditing: Boolean): Intent
        {
            val i = Intent(packageContext, PostPagerActivity::class.java)
            i.putExtra(EXTRA_POST_ID, postId)
            i.putExtra(EXTRA_ALLOW_INTENDED_SUBMIT_DATE_EDITING, allowIntendedSubmitDateEditing)
            return i
        }
    }
}

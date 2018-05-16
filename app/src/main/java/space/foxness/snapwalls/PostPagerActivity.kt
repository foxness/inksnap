package space.foxness.snapwalls

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import java.util.UUID

class PostPagerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private var posts: List<Post> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_pager)

        val postId = intent.getSerializableExtra(EXTRA_POST_ID) as UUID

        viewPager = findViewById(R.id.activity_post_pager_viewpager)

        posts = Queue.getInstance(this).posts
        val fm = supportFragmentManager
        viewPager.adapter = object : FragmentStatePagerAdapter(fm) {
            override fun getItem(position: Int): Fragment {
                val s = posts[position]
                return PostFragment.newInstance(s.id)
            }

            override fun getCount() = posts.size
        }

        for (i in posts.indices)
            if (posts[i].id == postId) {
                viewPager.currentItem = i
                break
            }
    }

    companion object {
        private const val EXTRA_POST_ID = "post_id"

        fun newIntent(packageContext: Context, postId: UUID): Intent {
            val i = Intent(packageContext, PostPagerActivity::class.java)
            i.putExtra(EXTRA_POST_ID, postId)
            return i
        }
    }
}
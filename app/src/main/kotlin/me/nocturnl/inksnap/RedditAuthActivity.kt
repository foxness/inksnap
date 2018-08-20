package me.nocturnl.inksnap

import android.content.Context
import android.content.Intent

class RedditAuthActivity : SingleFragmentActivity()
{
    override fun createFragment() = RedditAuthFragment.newInstance()

    override fun onBackPressed()
    {
        val redditAuthFragment = supportFragmentManager.findFragmentById(FRAGMENT_CONTAINER) as RedditAuthFragment
        
        if (!redditAuthFragment.processing)
        {
            super.onBackPressed()
        }
    }
    
    companion object
    {
        fun newIntent(context: Context) = Intent(context, RedditAuthActivity::class.java)
    }
}
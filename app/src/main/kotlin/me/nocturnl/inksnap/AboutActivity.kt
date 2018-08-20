package me.nocturnl.inksnap

import android.content.Context
import android.content.Intent

class AboutActivity : SingleFragmentActivity()
{
    override fun createFragment() = AboutFragment.newInstance()

    companion object
    {
        fun newIntent(context: Context) = Intent(context, AboutActivity::class.java)
    }
}
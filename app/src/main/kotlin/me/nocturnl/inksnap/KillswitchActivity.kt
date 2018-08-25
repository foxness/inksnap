package me.nocturnl.inksnap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class KillswitchActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_killswitch)
    }
    
    companion object
    {
        fun newIntent(context: Context) = Intent(context, KillswitchActivity::class.java)
    }
}
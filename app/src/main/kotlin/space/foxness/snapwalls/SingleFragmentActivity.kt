package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

abstract class SingleFragmentActivity : AppCompatActivity()
{
    protected abstract fun createFragment(): Fragment

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(SINGLE_FRAGMENT_LAYOUT)

        val fm = supportFragmentManager
        var fragment = fm.findFragmentById(FRAGMENT_CONTAINER)

        if (fragment == null)
        {
            fragment = createFragment()
            fm.beginTransaction().add(FRAGMENT_CONTAINER, fragment).commit()
        }
    }

    companion object
    {
        protected const val SINGLE_FRAGMENT_LAYOUT = R.layout.activity_fragment
        protected const val FRAGMENT_CONTAINER = R.id.fragment_container
    }
}

package space.foxness.snapwalls;

import android.support.v4.app.Fragment;

public class QueueActivity extends SingleFragmentActivity
{
    @Override
    protected Fragment createFragment()
    {
        return new QueueFragment();
    }
}

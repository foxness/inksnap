package space.foxness.snapwalls;

import android.support.v4.app.Fragment;

public class SubmissionActivity extends SingleFragmentActivity
{
    @Override
    protected Fragment createFragment()
    {
        return new SubmissionFragment();
    }
}

package space.foxness.snapwalls;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import java.util.UUID;

public class SubmissionActivity extends SingleFragmentActivity
{
    private static final String EXTRA_SUBMISSION_ID = "submission_id";
    
    @Override
    protected Fragment createFragment()
    {
        UUID submissionId = (UUID)getIntent().getSerializableExtra(EXTRA_SUBMISSION_ID);
        return SubmissionFragment.newInstance(submissionId);
    }
    
    public static Intent newIntent(Context packageContext, UUID submissionId)
    {
        Intent i = new Intent(packageContext, SubmissionActivity.class);
        i.putExtra(EXTRA_SUBMISSION_ID, submissionId);
        return i;
    }
}

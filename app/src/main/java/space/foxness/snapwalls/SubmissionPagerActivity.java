package space.foxness.snapwalls;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.List;
import java.util.UUID;

public class SubmissionPagerActivity extends AppCompatActivity
{
    private static final String EXTRA_SUBMISSION_ID = "submission_id";
    
    private ViewPager viewPager;
    private List<Submission> submissions;
    
    public static Intent newIntent(Context packageContext, UUID submissionId)
    {
        Intent i = new Intent(packageContext, SubmissionPagerActivity.class);
        i.putExtra(EXTRA_SUBMISSION_ID, submissionId);
        return i;
    }
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_pager);
        
        UUID submissionId = (UUID)getIntent().getSerializableExtra(EXTRA_SUBMISSION_ID);
        
        viewPager = findViewById(R.id.activity_submission_pager_viewpager);
        
        submissions = Queue.get().getSubmissions();
        FragmentManager fm = getSupportFragmentManager();
        viewPager.setAdapter(new FragmentStatePagerAdapter(fm)
        {
            @Override
            public Fragment getItem(int position)
            {
                Submission s = submissions.get(position);
                return SubmissionFragment.newInstance(s.getId());
            }

            @Override
            public int getCount()
            {
                return submissions.size();
            }
        });
        
        for (int i = 0; i < submissions.size(); ++i)
            if (submissions.get(i).getId().equals(submissionId))
            {
                viewPager.setCurrentItem(i);
                break;
            }
    }
}

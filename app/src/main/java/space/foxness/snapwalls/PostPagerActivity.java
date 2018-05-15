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

public class PostPagerActivity extends AppCompatActivity
{
    private static final String EXTRA_POST_ID = "post_id";
    
    private ViewPager viewPager;
    private List<Post> posts;
    
    public static Intent newIntent(Context packageContext, UUID postId)
    {
        Intent i = new Intent(packageContext, PostPagerActivity.class);
        i.putExtra(EXTRA_POST_ID, postId);
        return i;
    }
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_pager);
        
        UUID postId = (UUID)getIntent().getSerializableExtra(EXTRA_POST_ID);
        
        viewPager = findViewById(R.id.activity_post_pager_viewpager);
        
        posts = Queue.get().getPosts();
        FragmentManager fm = getSupportFragmentManager();
        viewPager.setAdapter(new FragmentStatePagerAdapter(fm)
        {
            @Override
            public Fragment getItem(int position)
            {
                Post s = posts.get(position);
                return PostFragment.newInstance(s.getId());
            }

            @Override
            public int getCount()
            {
                return posts.size();
            }
        });
        
        for (int i = 0; i < posts.size(); ++i)
            if (posts.get(i).getId().equals(postId))
            {
                viewPager.setCurrentItem(i);
                break;
            }
    }
}

package space.foxness.snapwalls;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;

import java.util.UUID;

public class PostFragment extends Fragment
{
    private static final String ARG_POST_ID = "post_id";

    private Post post;
    private boolean isValidPost; // todo: don't let user create invalid posts

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        UUID postId = (UUID)getArguments().getSerializable(ARG_POST_ID);
        post = Queue.get(getActivity()).getPost(postId);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Queue.get(getActivity()).updatePost(post);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_post, container, false);
        
        EditText titleET = v.findViewById(R.id.post_title);
        titleET.setText(post.getTitle());
        titleET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                post.setTitle(s.toString());
                updateIsPostValid();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        EditText contentET = v.findViewById(R.id.post_content);
        contentET.setText(post.getContent());
        contentET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                post.setContent(s.toString());
                updateIsPostValid();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        EditText subredditET = v.findViewById(R.id.post_subreddit);
        subredditET.setText(post.getSubreddit());
        subredditET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                post.setSubreddit(s.toString());
                updateIsPostValid();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });
        
        Switch typeSwitch = v.findViewById(R.id.post_type);
        typeSwitch.setChecked(post.getType());
        typeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> post.setType(isChecked));
        
        return v;
    }

    private void updateIsPostValid()
    {
        boolean validTitle = post.getTitle() != null && !post.getTitle().isEmpty();
        boolean validContent = post.getContent() != null && !post.getContent().isEmpty();
        boolean validSubreddit = post.getSubreddit() != null && !post.getSubreddit().isEmpty();

        isValidPost = validTitle && validContent && validSubreddit;
    }
    
    public static PostFragment newInstance(UUID postId)
    {
        Bundle args = new Bundle();
        args.putSerializable(ARG_POST_ID, postId);
        
        PostFragment fragment = new PostFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
package space.foxness.snapwalls;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

public class QueueFragment extends Fragment implements Reddit.Callbacks
{
    private static final String CONFIG_ACCESS_TOKEN = "accessToken";
    private static final String CONFIG_REFRESH_TOKEN = "refreshToken";
    private static final String CONFIG_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate";
    private static final String CONFIG_LAST_SUBMISSION_DATE = "lastSubmissionDate";

    private static final long CONFIG_NULL_SUBSTITUTE = 0;
    
    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private MenuItem signinMenuItem;

    private Reddit reddit;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        reddit = new Reddit(this);
        restoreConfig();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_queue, container, false);

        recyclerView = v.findViewById(R.id.queue_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        updateUI();

        return v;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateUI();
    }

    private void updateUI()
    {
        Queue queue = Queue.get();
        List<Post> posts = queue.getPosts();

        if (adapter == null)
        {
            adapter = new PostAdapter(posts);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_queue, menu);
        
        signinMenuItem = menu.findItem(R.id.menu_queue_signin);
        
        updateMenu();
    }

    private void AddNewPost()
    {
        Post s = new Post();
        s.setSubreddit("test"); // todo: change this
        Queue.get().addPost(s);
        startActivity(PostPagerActivity.newIntent(getActivity(), s.getId()));
    }
    
    private void ShowSigninDialog()
    {
        signinMenuItem.setEnabled(false);
        // we need ^ this because there's a token doesn't arrive immediately after the dialog is dismissed
        // and the user should not be able to press it when the token is being fetched
        
        Dialog authDialog = new Dialog(getActivity());
        authDialog.setContentView(R.layout.dialog_auth);
        authDialog.setOnCancelListener(dialog -> signinMenuItem.setEnabled(true));

        WebView authWebview = authDialog.findViewById(R.id.auth_webview);
        authWebview.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view, url);

                if (reddit.tryExtractCode(url))
                {
                    authDialog.dismiss();
                    reddit.fetchAuthTokens();
                }
            }
        });

        authWebview.loadUrl(reddit.getAuthorizationUrl());
        authDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_queue_add: AddNewPost(); return true;
            case R.id.menu_queue_signin: ShowSigninDialog(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void submit(Post s)
    {
        if (!reddit.canSubmitRightNow())
        {
            Toast.makeText(getActivity(), "Can't submit right now", Toast.LENGTH_SHORT).show();
            return;
        }

        reddit.submit(new Post(s));
    }

    @Override
    public void onTokenFetchFinish()
    {
        Toast.makeText(getActivity(), "fetched tokens, can post? " + reddit.canSubmitRightNow(), Toast.LENGTH_SHORT).show();
        updateMenu();
    }
    
    private void updateMenu()
    {
        signinMenuItem.setEnabled(!reddit.isSignedIn());
    }

    @Override
    public void onNewParams()
    {
        saveConfig();
        Toast.makeText(getActivity(), "SAVED THE CONFIG", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubmit(String link)
    {
        Toast.makeText(getActivity(), "GOT LINK: " + link, Toast.LENGTH_SHORT).show();
    }

    private void saveConfig()
    {
        Reddit.Params rp = reddit.getParams();
        long expirationDate = rp.getAccessTokenExpirationDate() == null ? CONFIG_NULL_SUBSTITUTE : rp.getAccessTokenExpirationDate().getTime();
        long lastSubmissionDate = rp.getLastSubmissionDate() == null ? CONFIG_NULL_SUBSTITUTE : rp.getLastSubmissionDate().getTime();

        SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        sp.edit()
                .putString(CONFIG_ACCESS_TOKEN, rp.getAccessToken())
                .putString(CONFIG_REFRESH_TOKEN, rp.getRefreshToken())
                .putLong(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, expirationDate)
                .putLong(CONFIG_LAST_SUBMISSION_DATE, lastSubmissionDate)
                .apply();
    }

    private void restoreConfig()
    {
        Reddit.Params rp = new Reddit.Params();

        SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        rp.setAccessToken(sp.getString(CONFIG_ACCESS_TOKEN, null));
        rp.setRefreshToken(sp.getString(CONFIG_REFRESH_TOKEN, null));

        Long dateInMs = sp.getLong(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, CONFIG_NULL_SUBSTITUTE);
        rp.setAccessTokenExpirationDate(dateInMs == CONFIG_NULL_SUBSTITUTE ? null : new Date(dateInMs));

        dateInMs = sp.getLong(CONFIG_LAST_SUBMISSION_DATE, CONFIG_NULL_SUBSTITUTE);
        rp.setLastSubmissionDate(dateInMs == CONFIG_NULL_SUBSTITUTE ? null : new Date(dateInMs));

        reddit.setParams(rp);
    }

    private class PostHolder extends RecyclerView.ViewHolder
    {
        private TextView titleTextView;
        private TextView contentTextView;
        private CheckBox typeCheckBox;

        private Post post;

        public PostHolder(View itemView)
        {
            super(itemView);
            itemView.setOnClickListener(this::onClick);

            titleTextView = itemView.findViewById(R.id.queue_post_title);
            contentTextView = itemView.findViewById(R.id.queue_post_content);
            typeCheckBox = itemView.findViewById(R.id.queue_post_type);
        }

        public void bindPost(Post s)
        {
            post = s;
            titleTextView.setText(post.getTitle());
            contentTextView.setText(post.getContent());
            typeCheckBox.setChecked(post.getType());
        }

        private void onClick(View v)
        {
            Intent i = PostPagerActivity.newIntent(getActivity(), post.getId());
            startActivity(i);
        }
    }

    private class PostAdapter extends RecyclerView.Adapter<PostHolder>
    {
        private List<Post> posts;

        public PostAdapter(List<Post> posts_)
        {
            posts = posts_;
        }

        @NonNull
        @Override
        public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.queue_post, parent, false);
            return new PostHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PostHolder holder, int position)
        {
            Post s = posts.get(position);
            holder.bindPost(s);
        }

        @Override
        public int getItemCount()
        {
            return posts.size();
        }
    }
}

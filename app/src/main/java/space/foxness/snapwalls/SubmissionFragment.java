package space.foxness.snapwalls;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Date;

public class SubmissionFragment extends Fragment implements Reddit.Callbacks
{
    private static final String CONFIG_ACCESS_TOKEN = "accessToken";
    private static final String CONFIG_REFRESH_TOKEN = "refreshToken";
    private static final String CONFIG_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate";
    private static final String CONFIG_LAST_SUBMISSION_DATE = "lastSubmissionDate";

    private static final long CONFIG_NULL_SUBSTITUTE = 0;

    private Button authButton;
    private Button submitButton;

    private Reddit reddit;
    private Submission submission;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        submission = new Submission();
        submission.setTitle("is testy besty?");
        submission.setType(false);
        submission.setContent("yes, testy is besty");
        submission.setSubreddit("test");

        reddit = new Reddit(this);
        
        restoreConfig();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_submission, container, false);
        
        submitButton = v.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v1 ->
        {
            if (!reddit.canSubmitRightNow())
            {
                Toast.makeText(getActivity(), "Can't submit right now", Toast.LENGTH_SHORT).show();
                return;
            }

            submitButton.setEnabled(false);
            reddit.submit(new Submission(submission));
        });

        authButton = v.findViewById(R.id.auth_button);
        authButton.setOnClickListener(v1 ->
        {
            authButton.setEnabled(false);
            Dialog authDialog = new Dialog(getActivity());
            authDialog.setContentView(R.layout.dialog_auth);

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
        });
        
        EditText titleET = v.findViewById(R.id.submission_title);
        titleET.setText(submission.getTitle());
        titleET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                submission.setTitle(s.toString());
                updateButtons();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        EditText contentET = v.findViewById(R.id.submission_content);
        contentET.setText(submission.getContent());
        contentET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                submission.setContent(s.toString());
                updateButtons();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        EditText subredditET = v.findViewById(R.id.submission_subreddit);
        subredditET.setText(submission.getSubreddit());
        subredditET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                submission.setSubreddit(s.toString());
                updateButtons();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });
        
        Switch typeSwitch = v.findViewById(R.id.submission_type);
        typeSwitch.setChecked(submission.getType());
        typeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> submission.setType(isChecked));

        updateButtons();
        
        return v;
    }

    private void updateButtons()
    {
        authButton.setEnabled(!reddit.isLoggedIn());
        
        boolean validTitle = submission.getTitle() != null && !submission.getTitle().isEmpty();
        boolean validContent = submission.getContent() != null && !submission.getContent().isEmpty();
        boolean validSubreddit = submission.getSubreddit() != null && !submission.getSubreddit().isEmpty();
        submitButton.setEnabled(validTitle && validContent && validSubreddit);
    }

    @Override
    public void onTokenFetchFinish()
    {
        Toast.makeText(getActivity(), "fetched tokens, can post? " + reddit.canSubmitRightNow(), Toast.LENGTH_SHORT).show();
        updateButtons();
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
        updateButtons();
    }

    private void saveConfig()
    {
        Reddit.Params rp = reddit.getParams();
        getActivity().getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
                .edit()
                .putString(CONFIG_ACCESS_TOKEN, rp.getAccessToken())
                .putString(CONFIG_REFRESH_TOKEN, rp.getRefreshToken())
                .putLong(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, rp.getAccessTokenExpirationDate() == null ? CONFIG_NULL_SUBSTITUTE : rp.getAccessTokenExpirationDate().getTime())
                .putLong(CONFIG_LAST_SUBMISSION_DATE, rp.getLastSubmissionDate() == null ? CONFIG_NULL_SUBSTITUTE : rp.getLastSubmissionDate().getTime())
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
}
package space.foxness.snapwalls;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import java.util.Date;

public class SubmitActivity extends AppCompatActivity implements Reddit.Callbacks
{
    private static final String CONFIG_ACCESS_TOKEN = "accessToken";
    private static final String CONFIG_REFRESH_TOKEN = "refreshToken";
    private static final String CONFIG_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate";
    private static final String CONFIG_LAST_SUBMISSION_DATE = "lastSubmissionDate";
    
    private static final long CONFIG_NULL_SUBSTITUTE = 0;
    
    private Button authButton;
    private Button submitButton;
    
    private Reddit reddit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);

        submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v ->
        {
            submitButton.setEnabled(false);
            reddit.submit();
            Toast.makeText(this, "Testy is besty!", Toast.LENGTH_SHORT).show();
        });
        
        authButton = findViewById(R.id.auth_button);
        authButton.setOnClickListener(v ->
        {
            authButton.setEnabled(false);
            Dialog authDialog = new Dialog(SubmitActivity.this);
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

        reddit = new Reddit(this);
        restoreConfig();
        
        updateButtons();
    }

    private void updateButtons()
    {
        authButton.setEnabled(!reddit.isLoggedIn());
        submitButton.setEnabled(reddit.canSubmitRightNow());
    }

    @Override
    public void onTokenFetchFinish()
    {
        Toast.makeText(this, "fetched tokens, can post? " + reddit.canSubmitRightNow(), Toast.LENGTH_SHORT).show();
        updateButtons();
    }

    @Override
    public void onNewParams()
    {
        saveConfig();
        Toast.makeText(this, "SAVED THE CONFIG", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubmit(String link)
    {
        Toast.makeText(this, "GOT LINK: " + link, Toast.LENGTH_SHORT).show();
        updateButtons();
    }

    private void saveConfig()
    {
        Reddit.Params rp = reddit.getParams();
        getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
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
        
        SharedPreferences sp = getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        rp.setAccessToken(sp.getString(CONFIG_ACCESS_TOKEN, null));
        rp.setRefreshToken(sp.getString(CONFIG_REFRESH_TOKEN, null));
        
        Long dateInMs = sp.getLong(CONFIG_ACCESS_TOKEN_EXPIRATION_DATE, CONFIG_NULL_SUBSTITUTE);
        rp.setAccessTokenExpirationDate(dateInMs == CONFIG_NULL_SUBSTITUTE ? null : new Date(dateInMs));
        
        dateInMs = sp.getLong(CONFIG_LAST_SUBMISSION_DATE, CONFIG_NULL_SUBSTITUTE);
        rp.setLastSubmissionDate(dateInMs == CONFIG_NULL_SUBSTITUTE ? null : new Date(dateInMs));
        
        reddit.setParams(rp);
    }
}

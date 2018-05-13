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
    private static final String REDDIT_ACCESS_TOKEN = "accessToken";
    private static final String REDDIT_REFRESH_TOKEN = "refreshToken";
    private static final String REDDIT_ACCESS_TOKEN_EXPIRATION_DATE = "accessTokenExpirationDate";
    
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
            reddit.ensureValidAccessToken();
            Toast.makeText(this, "Testy is besty!", Toast.LENGTH_SHORT).show();
        });
        
        authButton = findViewById(R.id.auth_button);
        authButton.setOnClickListener(v ->
        {
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
        authButton.setEnabled(!reddit.canSubmit());
        submitButton.setEnabled(reddit.canSubmit());
    }

    @Override
    public void onTokenFetchFinish()
    {
        Toast.makeText(this, "can I submit after token fetching? " + reddit.canSubmit(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNewParams()
    {
        saveConfig();
        Toast.makeText(this, "SAVED THE CONFIG", Toast.LENGTH_SHORT).show();
    }

    private void saveConfig()
    {
        Reddit.Params rp = reddit.getParams();
        getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
                .edit()
                .putString(REDDIT_ACCESS_TOKEN, rp.getAccessToken())
                .putString(REDDIT_REFRESH_TOKEN, rp.getRefreshToken())
                .putLong(REDDIT_ACCESS_TOKEN_EXPIRATION_DATE, rp.getAccessTokenExpirationDate().getTime())
                .apply();
    }
    
    private void restoreConfig()
    {
        Reddit.Params rp = new Reddit.Params();
        
        SharedPreferences sp = getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        rp.setAccessToken(sp.getString(REDDIT_ACCESS_TOKEN, null));
        rp.setRefreshToken(sp.getString(REDDIT_REFRESH_TOKEN, null));
        
        final long defaultValue = 0;
        Long dateInMs = sp.getLong(REDDIT_ACCESS_TOKEN_EXPIRATION_DATE, defaultValue);
        rp.setAccessTokenExpirationDate(dateInMs == defaultValue ? null : new Date(dateInMs));
        
        reddit.setParams(rp);
    }
}

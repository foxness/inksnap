package space.foxness.snapwalls;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

public class SubmitActivity extends AppCompatActivity
{
    private Reddit reddit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);

        Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v ->
        {
            Toast.makeText(this, "Testy is besty!", Toast.LENGTH_SHORT).show();
        });
        
        reddit = new Reddit();
        Button authButton = findViewById(R.id.auth_button);
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
                        authDialog.dismiss();
                }
            });
            
            authWebview.loadUrl(reddit.getAuthorizationUrl());
            authDialog.show();
        });
    }
}

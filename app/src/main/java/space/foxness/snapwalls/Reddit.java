package space.foxness.snapwalls;

import android.net.Uri;
import android.util.Log;

import com.loopj.android.http.*;

public class Reddit
{
    public final static String TAG = "!A!";
    
    private final static String APP_CLIENT_ID = "hSDlAP9u4cEFFA";
    private final static String APP_REDIRECT_URI = "http://localhost";
    private final static String AUTHORIZE_URL = "https://www.reddit.com/api/v1/authorize.compact";
    private final static String AUTH_DURATION = "permanent";
    private final static String AUTH_SCOPE = "submit";
    private final static String AUTH_RESPONSE_TYPE = "code";
    
    private String authState;
    private String authCode;
    
    public Reddit()
    {
//        new AsyncHttpClient();
    }
    
    private static String getRandomState()
    {
        // TODO: implement this method
        return "testy";
    }
    
    private void getTokens()
    {
//        Log.d(TAG, "CODE: " + authCode);
        // TODO: implement this method
    }
    
    public boolean tryExtractCode(String url)
    {
        if (!url.startsWith(APP_REDIRECT_URI) || authState == null)
            return false;

        Uri uri = Uri.parse(url);
        String state = uri.getQueryParameter("state");
        if (state == null || !state.equals(authState))
            return false;

        authState = null;
        authCode = uri.getQueryParameter("code");
        return authCode != null; // 'true' denotes success
    }
    
    public String getAuthorizationUrl()
    {
        authState = getRandomState();
        return Uri.parse(AUTHORIZE_URL).buildUpon()
                .appendQueryParameter("client_id", APP_CLIENT_ID)
                .appendQueryParameter("response_type", AUTH_RESPONSE_TYPE)
                .appendQueryParameter("state", authState)
                .appendQueryParameter("redirect_uri", APP_REDIRECT_URI)
                .appendQueryParameter("duration", AUTH_DURATION)
                .appendQueryParameter("scope", AUTH_SCOPE)
                .build().toString();
    }
}

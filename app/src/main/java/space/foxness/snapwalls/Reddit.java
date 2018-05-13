package space.foxness.snapwalls;

import android.net.Uri;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class Reddit
{
    public final static String TAG = "!A!";
    
    private final static String APP_CLIENT_ID = "hSDlAP9u4cEFFA";
    private final static String APP_CLIENT_SECRET = ""; // installed apps have no secrets
    private final static String APP_REDIRECT_URI = "http://localhost";
    
    private final static String AUTORIZE_ENDPOINT = "https://www.reddit.com/api/v1/authorize.compact";
    private final static String ACCESS_TOKEN_ENDPOINT = "https://www.reddit.com/api/v1/access_token";
    
    private final static String AUTH_DURATION = "permanent";
    private final static String AUTH_SCOPE = "submit";
    private final static String AUTH_RESPONSE_TYPE = "code";
    
    private String authState;
    private String authCode;
    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpirationDate;
    
    private Callbacks callbacks;
    
    public Reddit(Callbacks cbs)
    {
        callbacks = cbs;
    }
    
    public static class Params
    {
        private String accessToken;
        private String refreshToken;
        private Date accessTokenExpirationDate;

        public void setAccessToken(String accessToken)
        {
            this.accessToken = accessToken;
        }

        public void setRefreshToken(String refreshToken)
        {
            this.refreshToken = refreshToken;
        }

        public void setAccessTokenExpirationDate(Date accessTokenExpirationDate)
        {
            this.accessTokenExpirationDate = accessTokenExpirationDate;
        }
        
        public String getAccessToken()
        {
            return accessToken;
        }

        public String getRefreshToken()
        {
            return refreshToken;
        }

        public Date getAccessTokenExpirationDate()
        {
            return accessTokenExpirationDate;
        }
    }
    
    public Params getParams()
    {
        Params p = new Params();
        p.setAccessToken(accessToken);
        p.setRefreshToken(refreshToken);
        p.setAccessTokenExpirationDate(accessTokenExpirationDate);
        return p;
    }
    
    public void setParams(Params params)
    {
        accessToken = params.getAccessToken();
        refreshToken = params.getRefreshToken();
        accessTokenExpirationDate = params.getAccessTokenExpirationDate();
    }
    
    public interface Callbacks
    {
        void onTokenFetchFinish();
        void onNewParams();
    }
    
    private static String getRandomState()
    {
        // TODO: implement this method
        return "testy";
    }
    
    public boolean canSubmit()
    {
        return refreshToken != null;
    }
    
    public void ensureValidAccessToken() // TODO: change to private after you add 'submit' method
    {
        if (accessToken != null && accessTokenExpirationDate.after(new Date()))
            return; // access token is good
        
        // access token is null or expired
        
        assert refreshToken != null;
        
        AsyncHttpClient ahc = new AsyncHttpClient();
        ahc.setBasicAuth(APP_CLIENT_ID, APP_CLIENT_SECRET);

        RequestParams params = new RequestParams();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);
        ahc.post(ACCESS_TOKEN_ENDPOINT, params, new JsonHttpResponseHandler()
        {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response)
            {
                super.onSuccess(statusCode, headers, response);

                try
                {
                    // TODO: compare received scope with intended scope?
                    updateAccessToken(response.getString("access_token"), response.getInt("expires_in"));
                }
                catch (JSONException e)
                {
                    // TODO: handle this
                    throw new RuntimeException(e);
                }

                callbacks.onNewParams();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse)
            {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                
                // todo: handle this
                throw new RuntimeException(throwable);
            }
        });
    }
    
    private void updateAccessToken(String accessToken_, int expiresIn)
    {
        accessToken = accessToken_;
        Calendar date = Calendar.getInstance(); // current time
        date.add(Calendar.SECOND, expiresIn); // expiresIn == 1 hour
        accessTokenExpirationDate = date.getTime();
    }
    
    public void fetchAuthTokens()
    {
        assert authCode != null;
        
        AsyncHttpClient ahc = new AsyncHttpClient();
        ahc.setBasicAuth(APP_CLIENT_ID, APP_CLIENT_SECRET);

        RequestParams params = new RequestParams();
        params.add("grant_type", "authorization_code");
        params.add("code", authCode);
        params.add("redirect_uri", APP_REDIRECT_URI);
        ahc.post(ACCESS_TOKEN_ENDPOINT, params, new JsonHttpResponseHandler()
        {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response)
            {
                super.onSuccess(statusCode, headers, response);
                
                try
                {
                    // TODO: compare received scope with intended scope?
                    refreshToken = response.getString("refresh_token");
                    updateAccessToken(response.getString("access_token"), response.getInt("expires_in"));
                }
                catch (JSONException e)
                {
                    // TODO: handle this
                    throw new RuntimeException(e);
                }

                callbacks.onNewParams();
                callbacks.onTokenFetchFinish();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse)
            {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                
                // TODO: handle this
                throw new RuntimeException(throwable);
                
//                callbacks.onTokenFetchFinish();
            }
        });
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
        return Uri.parse(AUTORIZE_ENDPOINT).buildUpon()
                .appendQueryParameter("client_id", APP_CLIENT_ID)
                .appendQueryParameter("response_type", AUTH_RESPONSE_TYPE)
                .appendQueryParameter("state", authState)
                .appendQueryParameter("redirect_uri", APP_REDIRECT_URI)
                .appendQueryParameter("duration", AUTH_DURATION)
                .appendQueryParameter("scope", AUTH_SCOPE)
                .build().toString();
    }
}

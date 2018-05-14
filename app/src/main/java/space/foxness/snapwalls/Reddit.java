package space.foxness.snapwalls;

import android.net.Uri;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
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
    private final static String SUBMIT_ENDPOINT = "https://oauth.reddit.com/api/submit";
    
    private final static String AUTH_DURATION = "permanent";
    private final static String AUTH_SCOPE = "submit";
    private final static String AUTH_RESPONSE_TYPE = "code";
    private final static String USER_AGENT = "Snapwalls by /u/foxneZz";
    
    private final static int RATELIMIT = 10 * 60 * 1000; // in milliseconds, also 10 minutes
    
    private String authState;
    private String authCode;
    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpirationDate;
    private Date lastSubmissionDate;
    
    private Callbacks callbacks;
    
    public Reddit(Callbacks cbs)
    {
        callbacks = cbs;
    }

    public boolean canSubmitRightNow()
    {
        return isSignedIn() && !isRestrictedByRatelimit();
    }

    public static class Params
    {
        private String accessToken;
        private String refreshToken;
        private Date accessTokenExpirationDate;
        private Date lastSubmissionDate;

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

        public void setLastSubmissionDate(Date lastSubmissionDate)
        {
            this.lastSubmissionDate = lastSubmissionDate;
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

        public Date getLastSubmissionDate()
        {
            return lastSubmissionDate;
        }
    }
    
    public Params getParams()
    {
        Params p = new Params();
        p.setAccessToken(accessToken);
        p.setRefreshToken(refreshToken);
        p.setAccessTokenExpirationDate(accessTokenExpirationDate);
        p.setLastSubmissionDate(lastSubmissionDate);
        return p;
    }
    
    public void setParams(Params params)
    {
        accessToken = params.getAccessToken();
        refreshToken = params.getRefreshToken();
        accessTokenExpirationDate = params.getAccessTokenExpirationDate();
        lastSubmissionDate = params.getLastSubmissionDate();
    }
    
    public interface Callbacks
    {
        void onTokenFetchFinish();
        void onNewParams();
        void onSubmit(String link);
    }
    
    private static String getRandomState()
    {
        // TODO: implement this method
        return "testy";
    }
    
    public boolean isSignedIn()
    {
        return refreshToken != null;
    }
    
    public boolean isRestrictedByRatelimit()
    {
        return lastSubmissionDate != null && new Date().getTime() < lastSubmissionDate.getTime() + RATELIMIT;
    }
    
    public void submit(Submission submission)
    {
        submit(submission, true, true);
    }
    
    public void submit(Submission submission, boolean resubmit, boolean sendReplies)
    {
        boolean validTitle = submission.getTitle() != null && !submission.getTitle().isEmpty();
        boolean validContent = submission.getContent() != null && !submission.getContent().isEmpty();
        boolean validSubreddit = submission.getSubreddit() != null && !submission.getSubreddit().isEmpty();
        
        if (!validTitle || !validContent || !validSubreddit)
            throw new RuntimeException("Bad submission");
        
        ensureValidAccessToken(() -> // success callback
        {
            AsyncHttpClient ahc = new AsyncHttpClient();
            ahc.setUserAgent(USER_AGENT);
            ahc.addHeader("Authorization", "bearer " + accessToken);
            
            RequestParams params = new RequestParams();
            params.add("api_type", "json");
            params.add("kind", submission.getType() ? "link" : "self");
            params.add("resubmit", resubmit ? "true" : "false");
            params.add("sendreplies", sendReplies ? "true" : "false");
            params.add("sr", submission.getSubreddit());
            params.add(submission.getType() ? "url" : "text", submission.getContent());
            params.add("title", submission.getTitle());
            
            ahc.post(SUBMIT_ENDPOINT, params, new JsonHttpResponseHandler()
            {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response)
                {
                    super.onSuccess(statusCode, headers, response);
                    
                    response = response.optJSONObject("json");
                    JSONArray errors = response.optJSONArray("errors");
                    
                    if (errors.length() == 0)
                    {
                        lastSubmissionDate = new Date();
                        callbacks.onNewParams();
                        callbacks.onSubmit(response.optJSONObject("data").optString("url"));
                    }
                    else
                    {
                        StringBuilder errorString = new StringBuilder("You got " + errors.length() + " errors: ");
                        for (int i = 0; i < errors.length(); ++i)
                        {
                            JSONArray error = errors.optJSONArray(i);
                            String name = error.optString(0);
                            String description = error.optString(1);

                            switch (name)
                            {
                                case "RATELIMIT":
                                case "NO_URL":
                                    errorString.append(name).append(" ").append(description).append("\n"); break;
                                default: throw new RuntimeException("I FOUND AN UNKNOWN ERROR:\nNAME: " + name + "\nDESCRIPTION: " + description);
                            }
                        }
                        
                        callbacks.onSubmit(errorString.toString());
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse)
                {
                    super.onFailure(statusCode, headers, throwable, errorResponse);

                    // todo: handle this
                    throw new RuntimeException(throwable);
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString)
                {
                    super.onSuccess(statusCode, headers, responseString);

                    // todo: handle this
                    throw new RuntimeException("RESPONSE STRING ONSUCCESS");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable)
                {
                    super.onFailure(statusCode, headers, responseString, throwable);

                    // todo: handle this
                    throw new RuntimeException(throwable);
                }
            });
            
        }, () -> // error callback, happens when you couldn't get a good access token
        {
            // todo: handle this
            throw new RuntimeException();
        });
    }
    
    private void ensureValidAccessToken(Runnable successCallback, Runnable errorCallback)
    {
        if (accessToken != null && accessTokenExpirationDate.after(new Date()))
        {
            successCallback.run();
            return;
        }
        
        // access token is null or expired
        
        assert refreshToken != null;
        
        AsyncHttpClient ahc = new AsyncHttpClient();
        ahc.setUserAgent(USER_AGENT);
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
                successCallback.run();
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
        ahc.setUserAgent(USER_AGENT);
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

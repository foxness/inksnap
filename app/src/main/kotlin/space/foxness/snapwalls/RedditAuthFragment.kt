package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import space.foxness.snapwalls.Util.toast

class RedditAuthFragment : Fragment()
{
    private lateinit var authWebview: WebView
    
    var processing = false 
        private set
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val v = inflater.inflate(R.layout.fragment_auth, container, false)

        // todo: fix all of this mess by refactoring reddit
        val dummyCallbacks = object: Reddit.Callbacks
        {
            override fun onNewAccessToken() { }

            override fun onNewRefreshToken() { }

            override fun onNewLastSubmissionDate() { }

            override fun onNewName() { }
        }
        
        val redditAccount = Reddit.getInstance(dummyCallbacks)
        
        authWebview = v.findViewById(R.id.authentification_webview)

        authWebview.webViewClient = object : WebViewClient()
        {
            @SuppressLint("SetTextI18n") // todo: fixeroni
            override fun onPageFinished(view: WebView, url: String)
            {
                super.onPageFinished(view, url)

                if (redditAccount.tryExtractCode(url))
                {
                    processing = true
                    toast("Processing...")
                    
                    val fetchJob = launch {
                        redditAccount.fetchAuthTokens()
                        redditAccount.fetchName()
                    }

                    launch(UI) {
                        fetchJob.join()
                        processing = false
                        
                        val result = if (redditAccount.isLoggedIn)
                        {
                            Activity.RESULT_OK
                        }
                        else
                        {
                            Activity.RESULT_CANCELED
                        }
                        
                        activity!!.setResult(result)
                        activity!!.finish()
                    }
                }
            }
        }

        authWebview.loadUrl(redditAccount.authorizationUrl)
        
        return v
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        
        Util.clearCookiesAndCache(authWebview)
    }
    
    companion object
    {
        fun newInstance() = RedditAuthFragment()
    }
}
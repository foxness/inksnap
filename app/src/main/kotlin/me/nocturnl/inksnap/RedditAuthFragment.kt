package me.nocturnl.inksnap

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
import me.nocturnl.inksnap.Util.toast
import android.app.ProgressDialog
import android.widget.ProgressBar


class RedditAuthFragment : Fragment()
{
    private lateinit var authWebview: WebView
    
    var processing = false 
        private set
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_auth, container, false)
        
        val redditAccount = Autoreddit.getInstance(context!!).reddit
        
        val progressBarView = v.findViewById<ProgressBar>(R.id.progressbar_view)
        
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
                    authWebview.visibility = View.GONE
                    progressBarView.visibility = View.VISIBLE
                    
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
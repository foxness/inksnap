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
import android.widget.ProgressBar
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.nocturnl.inksnap.Reddit.UserResponse.Allow
import me.nocturnl.inksnap.Reddit.UserResponse.Decline


class RedditAuthFragment : Fragment()
{
    private lateinit var authWebview: WebView
    private lateinit var progressBarView: ProgressBar
    
    var processing = false 
        private set
    
    private fun setLoadingIndicatorVisibility(visible: Boolean)
    {
        authWebview.visibility = Util.getVisibilityGoneConstant(!visible)
        progressBarView.visibility = Util.getVisibilityGoneConstant(visible)
    }
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_auth, container, false)
        
        val redditAccount = Autoreddit.getInstance(context!!).reddit
        
        progressBarView = v.findViewById(R.id.progressbar_view)
        authWebview = v.findViewById(R.id.authentification_webview)
        
        setLoadingIndicatorVisibility(true)

        authWebview.webViewClient = object : WebViewClient()
        {
            @SuppressLint("SetTextI18n") // todo: fixeroni
            override fun onPageFinished(view: WebView, url: String)
            {
                super.onPageFinished(view, url)
                
                if (Reddit.isLoginUrl(url))
                {
                    setLoadingIndicatorVisibility(false)
                }
                else
                {
                    val userResponse = redditAccount.getUserResponse(url)
                    
                    // ignore this warning
                    // when userResponse is none, we shouldn't do anything
                    when (userResponse)
                    {
                        Allow ->
                        {
                            processing = true
                            setLoadingIndicatorVisibility(true)

                            val fetchJob = GlobalScope.launch {
                                redditAccount.fetchAuthTokens()
                                redditAccount.fetchName()
                            }

                            GlobalScope.launch {
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
                        
                        Decline ->
                        {
                            setLoadingIndicatorVisibility(true) // just for a split second
                            activity!!.setResult(Activity.RESULT_CANCELED)
                            activity!!.finish()
                        }
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
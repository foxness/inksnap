package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Switch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import space.foxness.snapwalls.Util.toast

class NewSettingsFragment : Fragment()
{
    private lateinit var redditButton: Button
    private lateinit var imgurButton: Button
    private lateinit var autosubmitTypeButton: Button
    
    private lateinit var settingsManager: SettingsManager
    private lateinit var redditAccount: Reddit
    private lateinit var imgurAccount: ImgurAccount

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        
        val ctx = context!!
        settingsManager = SettingsManager.getInstance(ctx)
        redditAccount = Autoreddit.getInstance(ctx).reddit
        imgurAccount = Autoimgur.getInstance(ctx).imgurAccount
    }
    
    @SuppressLint("SetTextI18n") // todo: fixeroni
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        val v = inflater.inflate(R.layout.fragment_new_settings, container, false)
        
        // REDDIT ACCOUNT --------------------
        
        redditButton = v.findViewById(R.id.reddit_toggle)
        redditButton.text = if (redditAccount.isLoggedIn) "Log out" else "Log in"
        redditButton.setOnClickListener { onRedditButtonClick() }

        // IMGUR ACCOUNT --------------------

        imgurButton = v.findViewById(R.id.imgur_toggle)
        imgurButton.text = if (imgurAccount.isLoggedIn) "Log out" else "Log in"
        imgurButton.setOnClickListener { onImgurButtonClick() }
        
        // DEBUG DONT POST ------------------
        
        val debugDontPostSwitch = v.findViewById<Switch>(R.id.debug_dont_post_switch)
        debugDontPostSwitch.isChecked = settingsManager.debugDontPost
        debugDontPostSwitch.setOnCheckedChangeListener { buttonView, isChecked -> onDebugDontPostCheckedChanged(isChecked) }
        
        // AUTOSUBMIT TYPE ------------------
        
        autosubmitTypeButton = v.findViewById(R.id.autosubmit_button)
        autosubmitTypeButton.setOnClickListener { onAutosubmitTypeButtonClick() }
        
        return v
    }

    override fun onStart()
    {
        super.onStart()
        autosubmitTypeButton.isEnabled = !settingsManager.autosubmitEnabled
        redditButton.isEnabled = !settingsManager.autosubmitEnabled
    }
    
    private fun onAutosubmitTypeButtonClick()
    {
        val types = arrayOf("Manual", "Periodic") // todo: extract hardcoded strings
        val checkedItem = settingsManager.autosubmitType.ordinal
        
        val adb = AlertDialog.Builder(context!!)
        
        adb.setTitle("Autosubmit type") // todo: same ^
        adb.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
        }
        adb.setSingleChoiceItems(types, checkedItem) { dialog: DialogInterface, which: Int ->
            settingsManager.autosubmitType = SettingsManager.AutosubmitType.values()[which]
            dialog.dismiss()
        }
        
        adb.show()
    }
    
    private fun onDebugDontPostCheckedChanged(checked: Boolean)
    {
        settingsManager.debugDontPost = checked // todo: save only on activity destroy?
    }
    
    @SuppressLint("SetTextI18n") // todo: fixeroni
    private fun onImgurButtonClick()
    {
        if (imgurAccount.isLoggedIn)
        {
            imgurAccount.logout()
            imgurButton.text = "Log in"
            toast("Logged out of Imgur")
        }
        else
        {
            showImgurLoginDialog()
        }
    }
    
    private fun showImgurLoginDialog()
    {
        val authDialog = Dialog(context!!)
        authDialog.setContentView(R.layout.dialog_auth)

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)

        authDialog.setOnDismissListener {
            Util.clearCookiesAndCache(authWebview)
            imgurButton.text = if (imgurAccount.isLoggedIn) "Log out" else "Log in"
            toast(if (imgurAccount.isLoggedIn) "Success" else "Fail")
        }

        authWebview.webViewClient = object : WebViewClient()
        {
            override fun onPageFinished(view: WebView, url: String)
            {
                super.onPageFinished(view, url)

                if (imgurAccount.tryExtractTokens(url))
                {
                    authDialog.dismiss()
                }
            }
        }

        authWebview.loadUrl(imgurAccount.authorizationUrl)
        authDialog.show()
    }
    
    @SuppressLint("SetTextI18n") // todo: fixeroni
    private fun onRedditButtonClick()
    {
        if (redditAccount.isLoggedIn)
        {
            if (settingsManager.autosubmitEnabled)
            {
                toast("Can't change account while posts are scheduled")
            }
            else
            {
                redditAccount.logout()
                redditButton.text = "Log in"
                toast("Logged out of Reddit")
            }
        }
        else
        {
            showRedditLoginDialog()
        }
    }
    
    private fun showRedditLoginDialog()
    {
        val authDialog = Dialog(context!!)
        authDialog.setContentView(R.layout.dialog_auth)

        authDialog.setOnCancelListener {
            toast("Fail")
        }

        val authWebview = authDialog.findViewById<WebView>(R.id.auth_webview)

        authDialog.setOnDismissListener {
            Util.clearCookiesAndCache(authWebview)
        }

        authWebview.webViewClient = object : WebViewClient()
        {
            @SuppressLint("SetTextI18n") // todo: fixeroni
            override fun onPageFinished(view: WebView, url: String)
            {
                super.onPageFinished(view, url)

                if (redditAccount.tryExtractCode(url))
                {
                    redditButton.isEnabled = false

                    authDialog.dismiss()

                    doAsync {
                        redditAccount.fetchAuthTokens()

                        uiThread {
                            redditButton.isEnabled = true
                            
                            toast(if (redditAccount.isLoggedIn) "Success" else "Fail")
                            
                            if (redditAccount.isLoggedIn)
                            {
                                redditButton.text = "Log out"
                            }
                        }
                    }
                }
            }
        }

        authWebview.loadUrl(redditAccount.authorizationUrl)
        authDialog.show()
    }
}
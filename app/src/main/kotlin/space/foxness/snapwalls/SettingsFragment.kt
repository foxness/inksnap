package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.joda.time.Duration
import space.foxness.snapwalls.Util.toast

class SettingsFragment : Fragment()
{
    private lateinit var redditButton: Button
    private lateinit var imgurButton: Button
    private lateinit var autosubmitTypeButton: Button
    private lateinit var timerPeriodButton: Button

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
        val v = inflater.inflate(R.layout.fragment_settings, container, false)

        // REDDIT ACCOUNT --------------------

        redditButton = v.findViewById(R.id.reddit_toggle)
        redditButton.text = if (redditAccount.isLoggedIn) "Log out" else "Log in"
        redditButton.setOnClickListener { onRedditButtonClick() }

        // IMGUR ACCOUNT ---------------------

        imgurButton = v.findViewById(R.id.imgur_toggle)
        imgurButton.text = if (imgurAccount.isLoggedIn) "Log out" else "Log in"
        imgurButton.setOnClickListener { onImgurButtonClick() }

        // DEBUG DONT POST -------------------

        val debugDontPostSwitch = v.findViewById<Switch>(R.id.debug_dont_post_switch)
        debugDontPostSwitch.isChecked = settingsManager.debugDontPost
        debugDontPostSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            onDebugDontPostCheckedChanged(isChecked)
        }

        // AUTOSUBMIT TYPE -------------------

        autosubmitTypeButton = v.findViewById(R.id.autosubmit_button)
        autosubmitTypeButton.setOnClickListener { onAutosubmitTypeButtonClick() }

        // TIMER PERIOD ----------------------

        timerPeriodButton = v.findViewById(R.id.timer_period_button)
        timerPeriodButton.setOnClickListener { onTimerPeriodButtonClick() }
        
        // WALLPAPER MODE --------------------
        
        val wallpaperModeSwitch = v.findViewById<Switch>(R.id.wallpaper_mode_switch)
        wallpaperModeSwitch.isChecked = settingsManager.wallpaperMode
        wallpaperModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            onWallpaperModeCheckedChanged(isChecked)
        }

        return v
    }

    override fun onStart()
    {
        super.onStart()

        val autosubmitNotEnabled = !settingsManager.autosubmitEnabled
        autosubmitTypeButton.isEnabled = autosubmitNotEnabled
        redditButton.isEnabled = autosubmitNotEnabled
        timerPeriodButton.isEnabled = settingsManager.autosubmitType ==
                SettingsManager.AutosubmitType.Periodic && autosubmitNotEnabled
    }
    
    private fun onWallpaperModeCheckedChanged(checked: Boolean)
    {
        settingsManager.wallpaperMode = checked
    }

    private fun onTimerPeriodButtonClick()
    {
        val ctx = context!!

        val currentMinutes = settingsManager.period.standardMinutes

        val input = EditText(ctx) // todo: switch to layout
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setRawInputType(Configuration.KEYBOARD_12KEY)
        input.setText(currentMinutes.toString())
        
        val periodDialog = AlertDialog.Builder(ctx)
                .setView(input)
                .setTitle("Timer period") // todo: extracteroni
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

        periodDialog.setOnShowListener { _ ->
            val positiveButton = periodDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = periodDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            
            positiveButton.setOnClickListener {
                val minutes = input.text.toString().toInt()
                val millis = minutes * Util.MILLIS_IN_MINUTE

                if (millis > Reddit.RATELIMIT_MS)
                {
                    periodDialog.dismiss()
                    val period = Duration(millis)
                    settingsManager.period = period
                }
                else
                {
                    toast("The period cannot be 10 minutes or less (a rule imposed by Reddit)")
                }
            }
            
            negativeButton.setOnClickListener {
                periodDialog.dismiss()
            }
        }
        
        periodDialog.show()
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
            dialog.dismiss()
            val type = SettingsManager.AutosubmitType.values()[which]
            settingsManager.autosubmitType = type

            // why? because if changed type then autosubmit must be off
            timerPeriodButton.isEnabled = type == SettingsManager.AutosubmitType.Periodic
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
                    val fetchJob = launch { redditAccount.fetchAuthTokens() }
                    
                    redditButton.isEnabled = false
                    
                    authDialog.dismiss()
                    
                    launch(UI) {
                        fetchJob.join()

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

        authWebview.loadUrl(redditAccount.authorizationUrl)
        authDialog.show()
    }
}
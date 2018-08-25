package me.nocturnl.inksnap

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
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
import android.widget.*
import org.joda.time.Duration
import me.nocturnl.inksnap.Util.toast

class SettingsFragment : Fragment()
{
    private lateinit var redditButton: Button
    private lateinit var imgurButton: Button
    private lateinit var schedulingTypeButton: Button
    private lateinit var timerPeriodButton: Button
    private lateinit var redditNameView: TextView
    private lateinit var explanationView: TextView
    private lateinit var explanationDivider: View

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

        // REDDIT BUTTON ---------------------

        redditButton = v.findViewById(R.id.reddit_toggle)
        redditButton.text = if (redditAccount.isLoggedIn) "Log out" else "Log in"
        redditButton.setOnClickListener { onRedditButtonClick() }
        // todo: make all other activities/fragments use this kind of on click listeners
        
        // REDDIT ACCOUNT --------------------
        
        redditNameView = v.findViewById(R.id.reddit_account_name)
        updateRedditName()

        // IMGUR BUTTON ----------------------

        imgurButton = v.findViewById(R.id.imgur_toggle)
        imgurButton.text = if (imgurAccount.isLoggedIn) "Log out" else "Log in"
        imgurButton.setOnClickListener { onImgurButtonClick() }

        // SCHEDULING TYPE -------------------

        schedulingTypeButton = v.findViewById(R.id.scheduling_type_button)
        schedulingTypeButton.setOnClickListener { onSchedulingTypeButtonClick() }

        // TIMER PERIOD ----------------------

        timerPeriodButton = v.findViewById(R.id.timer_period_button)
        timerPeriodButton.setOnClickListener { onTimerPeriodButtonClick() }
        
        // WALLPAPER MODE --------------------
        
        val wallpaperModeSwitch = v.findViewById<Switch>(R.id.wallpaper_mode_switch)
        wallpaperModeSwitch.isChecked = settingsManager.wallpaperMode
        wallpaperModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            onWallpaperModeCheckedChanged(isChecked)
        }
        
        // EXPLANATION -----------------------

        explanationView = v.findViewById(R.id.explanation_view)
        explanationDivider = v.findViewById(R.id.explanation_divider)
        
        // -----------------------------------
        
        val imgurAccountSetting = v.findViewById<RelativeLayout>(R.id.imgur_account_setting)
        val wallpaperModeSetting = v.findViewById<RelativeLayout>(R.id.wallpaper_mode_setting)
        val schedulingTypeSetting = v.findViewById<RelativeLayout>(R.id.scheduling_type_setting)
        val timerPeriodSetting = v.findViewById<RelativeLayout>(R.id.timer_period_setting)

        val imgurAccountSettingDivider = v.findViewById<View>(R.id.imgur_account_setting_divider)
        val wallpaperModeSettingDivider = v.findViewById<View>(R.id.wallpaper_mode_setting_divider)
        val schedulingTypeSettingDivider = v.findViewById<View>(R.id.scheduling_type_setting_divider)
        val timerPeriodSettingDivider = v.findViewById<View>(R.id.timer_period_setting_divider)

        val developerOptionsUnlocked = settingsManager.developerOptionsUnlocked
        val visibilityConstant = Util.getVisibilityGoneConstant(developerOptionsUnlocked)
        
        imgurAccountSetting.visibility = visibilityConstant
        wallpaperModeSetting.visibility = visibilityConstant
        schedulingTypeSetting.visibility = visibilityConstant
        timerPeriodSetting.visibility = visibilityConstant
        
        imgurAccountSettingDivider.visibility = visibilityConstant
        wallpaperModeSettingDivider.visibility = visibilityConstant
        schedulingTypeSettingDivider.visibility = visibilityConstant
        timerPeriodSettingDivider.visibility = visibilityConstant

        return v
    }

    override fun onStart()
    {
        super.onStart()

        // todo: move all of these to oncreate(). why are they here in the first place?
        val submissionEnabled = settingsManager.submissionEnabled
        schedulingTypeButton.isEnabled = !submissionEnabled
        redditButton.isEnabled = !submissionEnabled
        timerPeriodButton.isEnabled = settingsManager.schedulingType ==
                SettingsManager.SchedulingType.Periodic && !submissionEnabled
        
        val explanationVisibility = Util.getVisibilityGoneConstant(submissionEnabled)
        explanationView.visibility = explanationVisibility
        explanationDivider.visibility = explanationVisibility
    }
    
    @SuppressLint("SetTextI18n") // todo: fix
    private fun updateRedditName()
    {
        redditNameView.visibility = Util.getVisibilityGoneConstant(redditAccount.isLoggedIn)
        
        if (redditAccount.isLoggedIn)
        {
            redditNameView.text = "/u/" + redditAccount.name
        }
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

                periodDialog.dismiss() // todo: im pretty sure this is done automatically
                val period = Duration(millis)
                settingsManager.period = period
            }
            
            negativeButton.setOnClickListener {
                periodDialog.dismiss()
            }
        }
        
        periodDialog.show()
    }

    private fun onSchedulingTypeButtonClick()
    {
        val types = arrayOf("Manual", "Periodic") // todo: extract hardcoded strings
        val checkedItem = settingsManager.schedulingType.ordinal

        val adb = AlertDialog.Builder(context!!)

        adb.setTitle("Scheduling type") // todo: same ^
        adb.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
        }
        adb.setSingleChoiceItems(types, checkedItem) { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            val type = SettingsManager.SchedulingType.values()[which]
            settingsManager.schedulingType = type

            // why? because if changed type then submission must be off
            timerPeriodButton.isEnabled = type == SettingsManager.SchedulingType.Periodic
        }

        adb.show()
    }

    @SuppressLint("SetTextI18n") // todo: fixeroni
    private fun onImgurButtonClick()
    {
        if (Util.isNetworkAvailable(context!!))
        {
            if (imgurAccount.isLoggedIn)
            {
                imgurAccount.logout()
                imgurButton.text = "Log in"
            }
            else
            {
                showImgurLoginDialog()
            }
        }
        else
        {
            Util.showNoInternetMessage(context!!)
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
        if (Util.isNetworkAvailable(context!!))
        {
            if (redditAccount.isLoggedIn)
            {
                if (settingsManager.submissionEnabled)
                {
                    // todo: remove because this is unreachable because the button is disabled
                    toast("Can't change account while posts are scheduled")
                }
                else
                {
                    redditAccount.logout()
                    redditButton.text = "Log in"
                    updateRedditName()
                }
            }
            else
            {
                showRedditLoginDialog()
            }
        }
        else
        {
            Util.showNoInternetMessage(context!!)
        }
    }

    private fun showRedditLoginDialog()
    {
        val i = RedditAuthActivity.newIntent(context!!)
        startActivityForResult(i, REQUEST_CODE_REDDIT_AUTH)
    }

    @SuppressLint("SetTextI18n") // todo: fix
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when (requestCode)
        {
            REQUEST_CODE_REDDIT_AUTH ->
            {
                if (redditAccount.isLoggedIn)
                {
                    redditButton.text = "Log out"
                }

                updateRedditName()
            }
            
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
    
    companion object
    {
        private const val REQUEST_CODE_REDDIT_AUTH = 0
        
        fun newInstance() = SettingsFragment()
    }
}
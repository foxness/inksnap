package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat()
{
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference)
    {
        var dialogFragment: DialogFragment? = null
        if (preference is MinutesPreference)
        {
            dialogFragment = MinutesPreferenceFragmentCompat.newInstance(preference.key)
        }

        if (dialogFragment != null)
        {
            dialogFragment.setTargetFragment(this, REQUEST_CODE)
            dialogFragment.show(fragmentManager,
                                "android.support.v7.preference.PreferenceFragment.DIALOG")
        }
        else
        {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object
    {
        private const val REQUEST_CODE = 0
    }
}
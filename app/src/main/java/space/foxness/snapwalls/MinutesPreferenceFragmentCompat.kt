package space.foxness.snapwalls

import android.os.Bundle
import android.support.v7.preference.PreferenceDialogFragmentCompat
import android.view.View
import android.widget.EditText

class MinutesPreferenceFragmentCompat : PreferenceDialogFragmentCompat() {
    
    private lateinit var minutesEdit: EditText
    
    override fun onDialogClosed(positiveResult: Boolean) {
        if (!positiveResult)
            return
        
        val minutes = minutesEdit.text.toString().toInt()
        val preference = preference
        
        if (preference is MinutesPreference) {
            if (preference.callChangeListener(minutes)) {
                preference.minutes = minutes
            }
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        
        minutesEdit = view.findViewById(R.id.minutes_edit)
        
        var minutes: Int? = null
        val preference = preference
        if (preference is MinutesPreference) {
            minutes = preference.minutes
        }
        
        if (minutes != null)
            minutesEdit.setText(minutes.toString())
    }
    
    companion object {
        fun newInstance(key: String): MinutesPreferenceFragmentCompat {
            val args = Bundle(1)
            args.putString(ARG_KEY, key)
            
            val fragment = MinutesPreferenceFragmentCompat()
            fragment.arguments = args
            return fragment
        }
    }
}

/*
public static TimePreferenceDialogFragmentCompat newInstance(
        String key) {
    final TimePreferenceDialogFragmentCompat
            fragment = new TimePreferenceDialogFragmentCompat();
    final Bundle b = new Bundle(1);
    b.putString(ARG_KEY, key);
    fragment.setArguments(b);

    return fragment;
}
 */
package space.foxness.snapwalls

import android.content.Context
import android.content.res.TypedArray
import android.support.v7.preference.DialogPreference
import android.util.AttributeSet

class MinutesPreference(context: Context, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    
    private var _minutes = 0
    
    var minutes
        get() = _minutes
        set(value) {
            _minutes = value
            persistInt(value)
        }
    
    init {
        dialogLayoutResource = R.layout.dialog_minutespicker
        
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int)
        = a!!.getInt(index, 0)

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        minutes = if (restorePersistedValue) getPersistedInt(_minutes) else defaultValue as Int
    }
}
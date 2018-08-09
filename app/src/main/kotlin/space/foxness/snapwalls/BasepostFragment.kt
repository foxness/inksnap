package space.foxness.snapwalls

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

abstract class BasepostFragment : Fragment()
{
    protected abstract val layoutId: Int
    
    private lateinit var titleEdit: EditText
    private lateinit var subredditEdit: EditText
    private lateinit var intendedSubmitDateButton: Button

    protected var intendedSubmitDate: DateTime? = null

    protected lateinit var post: Post
    protected var allowIntendedSubmitDateEditing = false

    // todo: account for submitservice (aka freeze when the submission process is coming)
    // example: submit service submits and deletes a post while you're editing it
    // bad stuff will happen then
    // this also applies to all other activities/fragments

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val args = arguments!!
        post = args.getSerializable(ARG_POST) as Post
        allowIntendedSubmitDateEditing = args.getBoolean(ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING)
    }

    private fun updateIntendedSubmitDateButtonText()
    {
        intendedSubmitDateButton.text = if (intendedSubmitDate == null)
        {
            "Date not set"
        }
        else
        {
            DateTimeFormat.forPattern(DATETIME_FORMAT).print(intendedSubmitDate)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(layoutId, container, false)

        // LAYOUT -----------------------------

        val layout = v.findViewById<LinearLayout>(R.id.post_layout)
        layout.requestFocus()

        // TITLE EDIT -------------------------

        titleEdit = v.findViewById(R.id.post_title)
        titleEdit.setText(post.title)

        // SUBREDDIT EDIT ---------------------

        subredditEdit = v.findViewById(R.id.post_subreddit)
        subredditEdit.setText(post.subreddit)

        // INTENDED SUBMIT DATE BUTTON --------------

        intendedSubmitDate = post.intendedSubmitDate
        intendedSubmitDateButton = v.findViewById(R.id.intended_submit_date_button)

        if (allowIntendedSubmitDateEditing)
        {
            updateIntendedSubmitDateButtonText()

            intendedSubmitDateButton.setOnClickListener { _ ->
                val context = context!!
                val is24HourFormat = DateFormat.is24HourFormat(context)

                // todo: maybe show now + 1 hour or something?
                val dialogDatetime = intendedSubmitDate ?: DateTime.now()

                var newYear: Int? = null
                var newMonth: Int? = null
                var newDay: Int? = null
                var newHour: Int? = null
                var newMinute: Int? = null

                val tsl = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
                    newHour = hour
                    newMinute = minute
                }

                val dialogHour = dialogDatetime.hourOfDay
                val dialogMinute = dialogDatetime.minuteOfHour

                val timeDialog = TimePickerDialog(context, tsl, dialogHour, dialogMinute, is24HourFormat)

                var timeDialogCanceled = false

                timeDialog.setOnCancelListener {
                    timeDialogCanceled = true
                }

                timeDialog.setOnDismissListener {
                    if (!timeDialogCanceled)
                    {
                        intendedSubmitDate = DateTime(newYear!!, newMonth!!, newDay!!, newHour!!, newMinute!!)
                        updateIntendedSubmitDateButtonText()
                    }
                }

                val dsl = DatePickerDialog.OnDateSetListener { view, year, month, day ->
                    newYear = year
                    newMonth = month + 1 // DatePicker months start at 0
                    newDay = day
                }

                val dialogYear = dialogDatetime.year
                val dialogMonth = dialogDatetime.monthOfYear - 1 // reason = above
                val dialogDay = dialogDatetime.dayOfMonth

                val dateDialog = DatePickerDialog(context, dsl, dialogYear, dialogMonth, dialogDay)

                var dateDialogCanceled = false

                dateDialog.setOnCancelListener {
                    dateDialogCanceled = true
                }

                dateDialog.setOnDismissListener {
                    if (!dateDialogCanceled)
                    {
                        timeDialog.show()
                    }
                }

                dateDialog.show()
            }
        }
        else
        {
            intendedSubmitDateButton.visibility = View.GONE
            val divider = v.findViewById<View>(R.id.datetime_divider)
            divider.visibility = View.GONE
        }

        return v
    }

    protected open fun unloadViewsToPost()
    {
        post.title = titleEdit.text.toString()
        post.subreddit = subredditEdit.text.toString()
        post.intendedSubmitDate = intendedSubmitDate
    }
    
    fun getThePost(): Post
    {
        unloadViewsToPost()
        return post
    }

    companion object
    {
        private const val ARG_POST = "post"
        private const val ARG_NEW_POST = "new_post"
        private const val ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING = "allow_intended_submit_date_editing"

        private const val DATETIME_FORMAT = "yyyy/MM/dd EEE HH:mm" // todo: make dependent on region/locale
        
        fun newArguments(post: Post?, allowIntendedSubmitDateEditing: Boolean): Bundle
        {
            val args = Bundle()

            if (post != null)
            {
                args.putSerializable(ARG_POST, post)
                args.putBoolean(ARG_NEW_POST, false)
            }
            else
            {
                args.putBoolean(ARG_NEW_POST, true)
            }

            args.putBoolean(ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING, allowIntendedSubmitDateEditing)
            
            return args
        }
    }
}
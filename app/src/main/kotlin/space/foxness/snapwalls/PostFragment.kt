package space.foxness.snapwalls

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.InputType
import android.text.format.DateFormat
import android.view.*
import android.widget.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import space.foxness.snapwalls.Util.toast

class PostFragment : Fragment()
{
    private lateinit var titleEdit: EditText
    private lateinit var contentEdit: EditText
    private lateinit var contentLabel: TextView
    private lateinit var subredditEdit: EditText
    private lateinit var linkSwitch: Switch
    private lateinit var intendedSubmitDateButton: Button
    private lateinit var pasteButton: Button

    private var intendedSubmitDate: DateTime? = null

    private lateinit var post: Post
    private var newPost = false
    private var allowIntendedSubmitDateEditing = false

    // todo: account for submitservice
    // example: submit service submits and deletes a post while you're editing it
    // bad stuff will happen then
    // this also applies to all other activities/fragments

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val args = arguments!!
        if (args.getBoolean(ARG_NEW_POST))
        {
            newPost = true
            post = Post.newInstance()
            post.title = "testy" // todo: remove on production
            post.content = "besty" // same ^
            post.subreddit = "test" // same ^
        }
        else
        {
            post = args.getSerializable(ARG_POST) as Post
        }

        allowIntendedSubmitDateEditing = args.getBoolean(ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?)
    {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_post, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
    {
        return when (item!!.itemId)
        {
            R.id.menu_post_delete ->
            {
                deletePost()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deletePost()
    {
        if (newPost)
        {
            activity!!.setResult(Activity.RESULT_CANCELED)
        }
        else
        {
            val i = Intent()
            i.putExtra(RESULT_DELETED_POST_ID, post.id)
            activity!!.setResult(RESULT_CODE_DELETED, i)
        }

        activity!!.finish()
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
    
    @SuppressLint("SetTextI18n") // todo: fix this, extract hardcoded strings
    private fun updatePostType(isLink: Boolean)
    {
        if (isLink)
        {
            contentLabel.text = "URL"
            contentEdit.hint = "Enter the url of the post"
            pasteButton.visibility = View.VISIBLE
            contentEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        else
        {
            contentLabel.text = "Content"
            contentEdit.hint = "Enter the content of the post"
            pasteButton.visibility = View.GONE
            contentEdit.inputType = InputType.TYPE_CLASS_TEXT // todo: multiline content input
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_post, container, false)
        
        // LAYOUT -----------------------------
        
        val layout = v.findViewById<LinearLayout>(R.id.post_layout)
        layout.requestFocus()

        // TITLE EDIT -------------------------

        titleEdit = v.findViewById(R.id.post_title)
        titleEdit.setText(post.title)

        // CONTENT EDIT -----------------------

        contentEdit = v.findViewById(R.id.post_content)
        contentEdit.setText(post.content)
        
        // CONTENT LABEL ----------------------
        
        contentLabel = v.findViewById(R.id.content_label)
        
        // PASTE BUTTON -----------------------
        
        pasteButton = v.findViewById(R.id.paste_button)
        val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        pasteButton.setOnClickListener {
            if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription.hasMimeType(MIMETYPE_TEXT_PLAIN))
            {
                val pasteData = clipboard.primaryClip.getItemAt(0).text
                if (pasteData != null)
                {
                    contentEdit.setText(pasteData)
                }
            }
        }

        // SUBREDDIT EDIT ---------------------

        subredditEdit = v.findViewById(R.id.post_subreddit)
        subredditEdit.setText(post.subreddit)

        // LINK SWITCH ------------------------

        linkSwitch = v.findViewById(R.id.post_link)
        linkSwitch.isChecked = post.isLink
        linkSwitch.setOnCheckedChangeListener { cb: CompoundButton, checked: Boolean ->
            updatePostType(checked)
        }

        // INTENDED SUBMIT DATE BUTTON --------------

        intendedSubmitDate = post.intendedSubmitDate
        intendedSubmitDateButton = v.findViewById(R.id.intended_submit_date_button)
        
        if (allowIntendedSubmitDateEditing)
        {
            updateIntendedSubmitDateButtonText()

            intendedSubmitDateButton.setOnClickListener {
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
            val label = v.findViewById<TextView>(R.id.intended_submit_date_label)
            label.visibility = View.GONE
        }

        // SAVE BUTTON ------------------------

        val saveButton = v.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            unloadViewsToPost()
            
            if (post.isValid(allowIntendedSubmitDateEditing))
            {
                val data = Intent()
                data.putExtra(RESULT_POST, post)
                activity!!.setResult(Activity.RESULT_OK, data)
                activity!!.finish()
            }
            else
            {
                toast(post.reasonWhyInvalid(allowIntendedSubmitDateEditing))
            }
        }
        
        updatePostType(post.isLink)

        return v
    }

    private fun unloadViewsToPost()
    {
        post.title = titleEdit.text.toString()
        post.content = contentEdit.text.toString()
        post.subreddit = subredditEdit.text.toString()
        post.isLink = linkSwitch.isChecked
        post.intendedSubmitDate = intendedSubmitDate
    }

    companion object
    {
        private const val ARG_POST = "post"
        private const val ARG_NEW_POST = "new_post"
        private const val ARG_ALLOW_INTENDED_SUBMIT_DATE_EDITING =
                "allow_intended_submit_date_editing"
        private const val RESULT_POST = "post"
        private const val RESULT_DELETED_POST_ID = "deleted_post_id"

        private const val DATETIME_FORMAT =
                "yyyy/MM/dd EEE HH:mm" // todo: make dependent on region/locale
        
        const val RESULT_CODE_DELETED = 5

        fun getPostFromResult(data: Intent) = data.getSerializableExtra(RESULT_POST) as? Post

        fun getDeletedPostIdFromResult(data: Intent) = data.getStringExtra(RESULT_DELETED_POST_ID)!!

        fun newInstance(post: Post?, allowIntendedSubmitDateEditing: Boolean): PostFragment
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

            val fragment = PostFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
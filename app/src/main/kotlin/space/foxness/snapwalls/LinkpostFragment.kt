package space.foxness.snapwalls

import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText

class LinkpostFragment : BasepostFragment()
{
    override val layoutId = R.layout.fragment_linkpost
    
    private lateinit var urlEdit: EditText
    private lateinit var pasteButton: Button

    // todo: account for submitservice (aka freeze when the submission process is coming)
    // example: submit service submits and deletes a post while you're editing it
    // bad stuff will happen then
    // this also applies to all other activities/fragments

    override fun initUi(v: View)
    {
        super.initUi(v)

        // URL EDIT -----------------------

        urlEdit = v.findViewById(R.id.post_url)

        // PASTE BUTTON -----------------------

        pasteButton = v.findViewById(R.id.paste_button)
        val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        pasteButton.setOnClickListener {
            if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true)
            {
                val pasteData = clipboard.primaryClip?.getItemAt(0)?.text
                if (pasteData != null)
                {
                    urlEdit.setText(pasteData)
                }
            }
        }
    }

    override fun applyPostToViews()
    {
        super.applyPostToViews()

        if (post.isLink)
        {
            urlEdit.setText(post.content)
        }
    }

    override fun unloadViewsToPost()
    {
        super.unloadViewsToPost()
        
        post.content = urlEdit.text.toString()
        post.isLink = true
    }

    companion object
    {
        fun newInstance(post: Post?, allowIntendedSubmitDateEditing: Boolean): LinkpostFragment
        {
            val args = BasepostFragment.newArguments(post, allowIntendedSubmitDateEditing)
            
            val fragment = LinkpostFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
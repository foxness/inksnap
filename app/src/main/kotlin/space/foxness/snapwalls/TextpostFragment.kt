package space.foxness.snapwalls

import android.view.View
import android.widget.EditText

class TextpostFragment : BasepostFragment()
{
    override val layoutId = R.layout.fragment_textpost
    
    private lateinit var contentEdit: EditText

    // todo: account for submitservice (aka freeze when the submission process is coming)
    // example: submit service submits and deletes a post while you're editing it
    // bad stuff will happen then
    // this also applies to all other activities/fragments

    override fun initUi(v: View)
    {
        super.initUi(v)

        // CONTENT EDIT -----------------------

        contentEdit = v.findViewById(R.id.post_content)
    }

    override fun applyPostToViews()
    {
        super.applyPostToViews()

        if (!post.isLink)
        {
            contentEdit.setText(post.content)
        }
    }

    override fun unloadViewsToPost()
    {
        super.unloadViewsToPost()
        
        post.content = contentEdit.text.toString() // intentionally no trim
        post.isLink = false
    }

    companion object
    {
        fun newInstance(post: Post?, allowIntendedSubmitDateEditing: Boolean): TextpostFragment
        {
            val args = BasepostFragment.newArguments(post, allowIntendedSubmitDateEditing)
            
            val fragment = TextpostFragment()
            fragment.arguments = args
            
            return fragment
        }
    }
}
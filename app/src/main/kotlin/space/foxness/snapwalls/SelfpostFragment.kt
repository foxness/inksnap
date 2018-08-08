package space.foxness.snapwalls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

class SelfpostFragment : BasepostFragment()
{
    private lateinit var contentEdit: EditText

    // todo: account for submitservice (aka freeze when the submission process is coming)
    // example: submit service submits and deletes a post while you're editing it
    // bad stuff will happen then
    // this also applies to all other activities/fragments

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        // CONTENT EDIT -----------------------

        contentEdit = v.findViewById(R.id.post_content)
        contentEdit.setText(post.content)

        return v
    }

    override fun unloadViewsToPost()
    {
        super.unloadViewsToPost()
        
        post.content = contentEdit.text.toString()
    }

    companion object
    {
        fun newInstance(post: Post?, allowIntendedSubmitDateEditing: Boolean): SelfpostFragment
        {
            val args = BasepostFragment.newArguments(post, allowIntendedSubmitDateEditing)
            
            val fragment = SelfpostFragment()
            fragment.arguments = args
            
            return fragment
        }
    }
}
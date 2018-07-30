package space.foxness.snapwalls

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PostedFragment : Fragment()
{
//    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View
    {
        val v = inflater.inflate(R.layout.fragment_posted, container, false)
        
//        recyclerView = v.findViewById(R.id.posted_recyclerview)
//        recyclerView.layoutManager = LinearLayoutManager(context!!)
//        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context,
//                                                             DividerItemDecoration.VERTICAL))
        
        return v
    }
}
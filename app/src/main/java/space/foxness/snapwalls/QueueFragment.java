package space.foxness.snapwalls;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class QueueFragment extends Fragment
{
    private RecyclerView recyclerView;
    private SubmissionAdapter adapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_queue, container, false);
        
        recyclerView = v.findViewById(R.id.queue_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        
        updateUI();
        
        return v;
    }

    private void updateUI()
    {
        Queue queue = Queue.get();
        List<Submission> submissions = queue.getSubmissions();
        
        adapter = new SubmissionAdapter(submissions);
        recyclerView.setAdapter(adapter);
    }

    private class SubmissionHolder extends RecyclerView.ViewHolder
    {
        public TextView titleTextView;
        
        public SubmissionHolder(View itemView)
        {
            super(itemView);
            
            titleTextView = (TextView)itemView;
        }
    }
    
    private class SubmissionAdapter extends RecyclerView.Adapter<SubmissionHolder>
    {
        private List<Submission> submissions;
        
        public SubmissionAdapter(List<Submission> queue_)
        {
            submissions = queue_;
        }

        @NonNull
        @Override
        public SubmissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new SubmissionHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SubmissionHolder holder, int position)
        {
            Submission s = submissions.get(position);
            holder.titleTextView.setText(s.getTitle());
        }

        @Override
        public int getItemCount()
        {
            return submissions.size();
        }
    }
}

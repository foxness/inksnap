package space.foxness.snapwalls;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

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

    @Override
    public void onResume()
    {
        super.onResume();
        updateUI();
    }

    private void updateUI()
    {
        Queue queue = Queue.get();
        List<Submission> submissions = queue.getSubmissions();
        
        if (adapter == null)
        {
            adapter = new SubmissionAdapter(submissions);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            adapter.notifyDataSetChanged();
        }
    }

    private class SubmissionHolder extends RecyclerView.ViewHolder
    {
        private TextView titleTextView;
        private TextView contentTextView;
        private CheckBox typeCheckBox;
        
        private Submission submission;
        
        public SubmissionHolder(View itemView)
        {
            super(itemView);
            itemView.setOnClickListener(this::onClick);
            
            titleTextView = itemView.findViewById(R.id.queue_submission_title);
            contentTextView = itemView.findViewById(R.id.queue_submission_content);
            typeCheckBox = itemView.findViewById(R.id.queue_submission_type);
        }

        public void bindSubmission(Submission s)
        {
            submission = s;
            titleTextView.setText(submission.getTitle());
            contentTextView.setText(submission.getContent());
            typeCheckBox.setChecked(submission.getType());
        }
        
        private void onClick(View v)
        {
            Intent i = SubmissionPagerActivity.newIntent(getActivity(), submission.getId());
            startActivity(i);
        }
    }
    
    private class SubmissionAdapter extends RecyclerView.Adapter<SubmissionHolder>
    {
        private List<Submission> submissions;
        
        public SubmissionAdapter(List<Submission> submissions_)
        {
            submissions = submissions_;
        }

        @NonNull
        @Override
        public SubmissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.queue_submission, parent, false);
            return new SubmissionHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SubmissionHolder holder, int position)
        {
            Submission s = submissions.get(position);
            holder.bindSubmission(s);
        }

        @Override
        public int getItemCount()
        {
            return submissions.size();
        }
    }
}

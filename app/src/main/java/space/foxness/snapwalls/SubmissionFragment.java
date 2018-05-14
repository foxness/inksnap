package space.foxness.snapwalls;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Date;
import java.util.UUID;

public class SubmissionFragment extends Fragment
{
    private static final String ARG_SUBMISSION_ID = "submission_id";

    private Submission submission;
    private boolean isValidSubmission; // todo: don't let user create invalid submissions

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        UUID submissionId = (UUID)getArguments().getSerializable(ARG_SUBMISSION_ID);
        submission = Queue.get().getSubmission(submissionId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_submission, container, false);
        
        EditText titleET = v.findViewById(R.id.submission_title);
        titleET.setText(submission.getTitle());
        titleET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                submission.setTitle(s.toString());
                updateIsSubmissionValid();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        EditText contentET = v.findViewById(R.id.submission_content);
        contentET.setText(submission.getContent());
        contentET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                submission.setContent(s.toString());
                updateIsSubmissionValid();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        EditText subredditET = v.findViewById(R.id.submission_subreddit);
        subredditET.setText(submission.getSubreddit());
        subredditET.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                submission.setSubreddit(s.toString());
                updateIsSubmissionValid();
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });
        
        Switch typeSwitch = v.findViewById(R.id.submission_type);
        typeSwitch.setChecked(submission.getType());
        typeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> submission.setType(isChecked));
        
        return v;
    }

    private void updateIsSubmissionValid()
    {
        boolean validTitle = submission.getTitle() != null && !submission.getTitle().isEmpty();
        boolean validContent = submission.getContent() != null && !submission.getContent().isEmpty();
        boolean validSubreddit = submission.getSubreddit() != null && !submission.getSubreddit().isEmpty();

        isValidSubmission = validTitle && validContent && validSubreddit;
    }
    
    public static SubmissionFragment newInstance(UUID submissionId)
    {
        Bundle args = new Bundle();
        args.putSerializable(ARG_SUBMISSION_ID, submissionId);
        
        SubmissionFragment fragment = new SubmissionFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
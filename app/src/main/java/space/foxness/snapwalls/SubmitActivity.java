package space.foxness.snapwalls;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SubmitActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);

        Button submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v ->
        {
            Toast.makeText(this, "Testy is besty!", Toast.LENGTH_LONG).show();
        });
    }
}

package net.thehecht.colecionador;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class NewPostActivity extends AppCompatActivity {

    private EditText textField;
    private View progressView;
    private View loginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        findViewById(R.id.send_new_post_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                String text = ((TextView)findViewById(R.id.text)).getText().toString().trim();
                if (text.isEmpty()) {
                    Snackbar.make(view, "É preciso digitar um texto", Snackbar.LENGTH_LONG).show();
                    return;
                }
                findViewById(R.id.form).setVisibility(View.GONE);
                findViewById(R.id.progress).setVisibility(View.VISIBLE);

                final DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                final String key = db.child("posts").push().getKey();
                final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Map postValues = new HashMap();
                postValues.put("user_id", userId);
                postValues.put("text", text);
                postValues.put("image", null);
                postValues.put("likes_count", 0);
                postValues.put("comments_count", 0);
                postValues.put("created_at", ServerValue.TIMESTAMP);
                Map updateValues = new HashMap();
                updateValues.put("posts/" + key, postValues);
                updateValues.put("user_posts/" + userId + "/" + key, postValues);
                db.updateChildren(updateValues, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Snackbar.make(view, "Ocorreu um erro ao adicionar o post.", Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        findViewById(R.id.form).setVisibility(View.VISIBLE);
                        finish();
                    }
                });



            }
        });

    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

}


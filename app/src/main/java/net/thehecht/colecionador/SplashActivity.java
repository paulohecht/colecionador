package net.thehecht.colecionador;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        FirebaseAuth.getInstance()
                .signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(SplashActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            return;
                            //TODO: REDIRECT TO FAIL SCREEN...
                        }

                        final DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
                        db.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.exists()) {
                                    //Set default values for user...
                                    HashMap users = new HashMap();
                                    users.put("name", "anonymous");
                                    users.put("likes_count", 0);
                                    users.put("posts_count", 0);
                                    db.setValue(users);
                                }
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                finish();
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                            }
                        });
                    }
                });
    }
}

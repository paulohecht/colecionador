package net.thehecht.colecionador;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NewPostActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private File tempFile = null;
    private File cropTempFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

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

                saveData(db, key, userId, view, text, new OnSaveCompletedListener() {
                    @Override
                    public void onSaveCompleted() {
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        findViewById(R.id.form).setVisibility(View.VISIBLE);
                        finish();
                    }
                });
            }
        });

    }

    private void saveData(final DatabaseReference db, final String key, final String userId, final View view, String text, final OnSaveCompletedListener callback) {
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
                uploadFile(db, key, userId, view, callback);
            }
        });
        FirebaseAnalytics.getInstance(NewPostActivity.this).logEvent("post", null);
    }

    private void uploadFile(final DatabaseReference db, final String key, final String userId, final View view, final OnSaveCompletedListener callback) {
        final StorageReference photoRef = FirebaseStorage.getInstance().getReference().child("posts").child(key + ".jpg");
        photoRef.putFile(Uri.fromFile(cropTempFile))
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        StorageMetadata metadata = taskSnapshot.getMetadata();
                        if (metadata == null) return;
                        Uri downloadUrl = metadata.getDownloadUrl();
                        if (downloadUrl == null) return;
                        String downloadPath = downloadUrl.toString();
                        Map<String, Object> postValues = new HashMap<String, Object>();
                        postValues.put("image", downloadPath);
                        postValues.put("updated_at", ServerValue.TIMESTAMP);
                        db.child("posts").child(key).updateChildren(postValues);
                        db.child("user_posts").child(userId).child(key).updateChildren(postValues);

                        callback.onSaveCompleted();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Snackbar.make(view, "Houve um erro ao salvar o post...", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(NewPostActivity.this.getPackageManager()) != null) {
            try {
                tempFile = File.createTempFile("temp", ".jpg", NewPostActivity.this.getCacheDir());
            } catch (IOException ex) {
                Snackbar.make(findViewById(android.R.id.content), "Houve um erro ao criar o arquivo temporário...", Snackbar.LENGTH_LONG).show();
            }
            if (tempFile != null) {
                Uri tempFileUri = FileProvider.getUriForFile(NewPostActivity.this, "net.thehecht.colecionador.fileprovider", tempFile);
                Log.d("DEBUG", tempFileUri.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (tempFile == null) return;
            UCrop.Options options = new UCrop.Options();
            options.setHideBottomControls(true);
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(80);
            try {
                cropTempFile = File.createTempFile("temp_crop", ".jpg", NewPostActivity.this.getCacheDir());
            } catch (IOException e) {
                Snackbar.make(findViewById(android.R.id.content), "Houve um erro ao criar o arquivo temporário...", Snackbar.LENGTH_LONG).show();
            }
            UCrop.of(Uri.fromFile(tempFile), Uri.fromFile(cropTempFile))
                    .withAspectRatio(600, 400)
                    .withMaxResultSize(600, 400)
                    .withOptions(options)
                    .start(this);
        }
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            final Uri resultUri = UCrop.getOutput(data);
            ((ImageView)findViewById(R.id.image)).setImageURI(resultUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Snackbar.make(findViewById(android.R.id.content), "Houve um erro ao recortar a foto...", Snackbar.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    public interface OnSaveCompletedListener {
        public void onSaveCompleted();
    }

}


package com.ufl.mybuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private final static int Gallery_Pick = 1;
    private Toolbar mToolbar;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mUser_id;
    private DocumentReference mUserRef;
    private TextView mBuddyNameTextView;
    private CircleImageView mUserProfileImage;
    private StorageReference mUserProfileImageRef;
    private String mName, mEmail, mBuddyName;
    private Uri mPhoto;
    private Button mUpdateName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mUserProfileImageRef = FirebaseStorage.getInstance().getReference().child("profile_images");

        if (mFirebaseUser != null) {
            mUser_id = mFirebaseUser.getUid();
            mName = mFirebaseUser.getDisplayName();
            mEmail = mFirebaseUser.getEmail();
            mPhoto = mFirebaseUser.getPhotoUrl();
        }
        mUserRef = db.collection("users").document(mUser_id);

        Log.d(TAG, "User Id: " + mUser_id + ".");
        Log.d(TAG, "User Name: " + mName + ".");
        Log.d(TAG, "Photo URI: " + mPhoto.toString() + ".");


        DocumentReference getBuddyName = db
                .collection("users")
                .document(mUser_id);

        getBuddyName.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    String name = document.getData().get("name").toString();
                    Log.d(TAG, "Buddies name: " + name);
                    mBuddyName = name;
                    InitializeFields(mBuddyName);
                } else {
                    Log.d(TAG, "Couldn't get name. Probably shouldn't be in MainActivity");
                    InitializeFields("No Name Found for Buddy :(");
                }
            }
        });

        mUpdateName = (Button) findViewById(R.id.settings_save);
        mUpdateName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> name = new HashMap<>();
                name.put("name", mBuddyNameTextView.getText().toString().trim());
                getBuddyName.update(name).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            hideKeyboard(SettingsActivity.this);
                            mBuddyNameTextView.clearFocus();
                            Toast.makeText(SettingsActivity.this, "Buddies Name Updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Buddies Name Failed to Update! :(", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Gallery_Pick && resultCode == RESULT_OK && data != null) {
            mPhoto = data.getData();

            StorageReference filePath = mUserProfileImageRef.child(mUser_id + ".jpg");

            filePath.putFile(mPhoto).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final String downloadUrl = uri.toString();
                                Log.d(TAG, "downloadURL: " + downloadUrl);
                                Log.d(TAG, "mPhoto.toString(): " + mPhoto.toString());
                                Map<String, Object> map = new HashMap<>();
                                map.put("profile_image", downloadUrl);
                                mUserRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Successfully uploaded image URI to firestore");
                                            Picasso.with(SettingsActivity.this).load(downloadUrl).placeholder(R.drawable.doggo).into(mUserProfileImage);
                                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                    .setPhotoUri(Uri.parse(downloadUrl))
                                                    .build();
                                            mFirebaseUser.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Log.d(TAG, "User profile updated");
                                                    }
                                                }
                                            });

                                        } else {
                                            String message = task.getException().getMessage();
                                            Log.e(TAG, "Erorr: " + message);
                                        }
                                    }
                                });
                                //Do what you want with the url
                            }
                        });
                    }
                }
            });
        }

    }


    private void InitializeFields(String name) {
        mBuddyNameTextView = (EditText) findViewById(R.id.buddy_full_name);
        mUserProfileImage = findViewById(R.id.buddy_picture);
        mBuddyNameTextView.setText(name);
        Picasso.with(SettingsActivity.this).load(mPhoto.toString()).placeholder(R.drawable.doggo).into(mUserProfileImage);
        mUserProfileImage.setVisibility(View.VISIBLE);

        mUserProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery_Pick);
            }
        });
    }

    // For closing the keyboard after Update Settings is tapped.
    // https://stackoverflow.com/questions/1109022/close-hide-android-soft-keyboard
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
        // To Clear back stack.
        // https://stackoverflow.com/questions/5794506/android-clear-the-back-stack
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivity);
        finish();
    }
}
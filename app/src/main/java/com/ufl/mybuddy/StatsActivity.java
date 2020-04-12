package com.ufl.mybuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.ogasimli.healthbarview.HealthBarView;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class StatsActivity extends AppCompatActivity {

    private static final String TAG = "StatsActivity";
    private Toolbar mToolbar;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mUser_id;
    private DocumentReference mUserRef;
    private TextView mBuddyNameTextView, mBuddyDateTextView;
    private CircleImageView mUserProfileImage;
    private StorageReference mUserProfileImageRef;
    private String mName, mEmail, mBuddyName;
    private Uri mPhoto;


    // Hunger and Thirst Bars
    private HealthBarView mHungerBar;
    private HealthBarView mThirstBar;
    private int mHunger, mThirst;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Stats");

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

        DocumentReference getValues = db
                .collection("users")
                .document(mUser_id);

        getValues.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    String name = document.getData().getOrDefault("name", "Buddy's Name Not Found").toString();
                    Log.d(TAG, "Buddies name: " + name);
                    Timestamp timestamp = (Timestamp) document.getData().getOrDefault("created", 0);
                    int hunger = (int) (long) document.getData().getOrDefault("hungerValue", 0);
                    int thirst = (int) (long) document.getData().getOrDefault("hungerValue", 0);
                    Log.d(TAG, "timestamp statsActivity: " + timestamp.toString());
                    mBuddyName = name;
                    Timestamp mBuddyCreated = timestamp;
                    InitializeFields(mBuddyName, hunger, thirst, timestamp);
                } else {
                    Log.d(TAG, "Couldn't get name. Probably shouldn't be in StatsActivity");
                    InitializeFields("No Name Found for Buddy :(", 0, 0, null);
                }
            }
        });
    }


    private void InitializeFields(String name, int hunger, int thirst, Timestamp timestamp) {
        mBuddyNameTextView = (TextView) findViewById(R.id.buddy_full_name);
        mBuddyDateTextView = (TextView) findViewById(R.id.buddy_date);
        mUserProfileImage = findViewById(R.id.buddy_picture);

        mBuddyNameTextView.setText(name);
        if (timestamp != null) {
            Date current = new Date();
            Date created = timestamp.toDate();
            long difference = current.getTime() - created.getTime();

            mBuddyDateTextView.setText("has been here for " + TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS) + " Days.");
        } else {
            mBuddyDateTextView.setText("");
        }
        Picasso.with(StatsActivity.this).load(mPhoto.toString()).placeholder(R.drawable.doggo).into(mUserProfileImage);

        mHungerBar = findViewById(R.id.hunger_bar);
        mThirstBar = findViewById(R.id.thirst_bar);
        mHungerBar.setValue(hunger);
        mThirstBar.setValue(thirst);

    }

}

package com.ufl.mybuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ufl.mybuddy.Intro.IntroActivity;

public class WelcomeActivity extends AppCompatActivity {
    private static int SPLASH_TIME_OUT = 3000;
    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Allows screen to change to next activity.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //After SPLASH_TIME_OUT ms, go to the character creation activity.

                // Initialize Firebase Auth
                FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

                // Check if user is signed in (non-null) and update UI accordingly.
                FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

                if (mFirebaseUser == null) {
                    // Not logged in, launch the Log In activity
                    Intent login = new Intent(WelcomeActivity.this, LogInActivity.class);
                    startActivity(login);
                } else {

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference docRef = db
                            .collection("users")
                            .document(mFirebaseUser.getUid());
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                    String name = document.getData().get("name").toString();
                                    if (!name.equals("")) {
                                        // Logged in, start Main Activity.
                                        Log.d(TAG, "Logged in, start Main Activity.");
                                        Intent main = new Intent(WelcomeActivity.this, MainActivity.class);
                                        startActivity(main);
                                    } else {
                                        // Logged in, have not set name yet. Start IntroActivity.
                                        Log.d(TAG, "Logged in, have not set name yet. Start IntroActivity.");
                                        Intent main = new Intent(WelcomeActivity.this, IntroActivity.class);
                                        startActivity(main);
                                    }
                                } else {
                                    // Logged in, have not set name yet. Start IntroActivity.
                                    Log.d(TAG, "Logged in, have not set name yet. Start IntroActivity.");
                                    Intent main = new Intent(WelcomeActivity.this, IntroActivity.class);
                                    startActivity(main);
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                                // Failed to connect to DB, start Login Activity.
                                Log.d(TAG, "Failed to connect to DB, start Login Activity.");
                                Intent main = new Intent(WelcomeActivity.this, LogInActivity.class);
                                startActivity(main);
                            }
                        }
                    });
                }
            }
        }, SPLASH_TIME_OUT);
    }
}

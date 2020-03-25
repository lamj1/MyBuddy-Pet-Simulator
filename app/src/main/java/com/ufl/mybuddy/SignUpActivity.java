package com.ufl.mybuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.ufl.mybuddy.Intro.IntroActivity;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    protected Button signUpButton;
    protected EditText fullName;
    protected EditText emailEditText;
    protected EditText passwordEditText;
    protected EditText passwordConfirmationEditText;

    private FirebaseAuth mFirebaseAuth;

    private String mName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mFirebaseAuth = FirebaseAuth.getInstance();

        fullName = findViewById(R.id.nameField);
        signUpButton = findViewById(R.id.signUpButton);
        emailEditText = findViewById(R.id.emailField);
        passwordEditText = findViewById(R.id.passwordField);
        passwordConfirmationEditText = findViewById(R.id.passwordConfirmationField);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mName = fullName.getText().toString();
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String passwordConfirmation = passwordConfirmationEditText.getText().toString();

                email = email.trim();
                //password = password.trim();
                //passwordConfirmation = passwordConfirmation.trim();

                if (mName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                    builder.setMessage(R.string.login_error_message_1)
                            .setTitle(R.string.login_error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else if (!password.equals(passwordConfirmation)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                    builder.setMessage(R.string.login_error_message_2)
                            .setTitle(R.string.login_error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    Log.d(TAG, "onCreate: passwords don't match. pass: " + password + " pass comfirmation: " + passwordConfirmation + " .");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    mFirebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "onCreate: success");
                                        FirebaseUser user = mFirebaseAuth.getCurrentUser();

                                        // Got below code from https://stackoverflow.com/questions/38114358/firebase-setdisplayname-of-user-while-creating-user-android
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(mName).build();

                                        user.updateProfile(profileUpdates)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Log.d(TAG, "onCreate: Name updated to profile.");
                                                        } else {
                                                            Log.e(TAG, "onCreate: **FAILURE** Name updated to profile.");
                                                        }
                                                    }
                                                });
//                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
//                                        DocumentReference newUser = db
//                                                .collection("users")
//                                                .document(user.getUid());
//                                        HashMap<String, String> userMap = new HashMap<String, String>();
//                                        userMap.put("privacy_default", "friends");
//                                        userMap.put("fullName", mName);
//                                        userMap.put("user_id", newUser.getId());
//                                        if (user.getPhotoUrl() != null) {
//                                            userMap.put("profile_image", user.getPhotoUrl().toString());
//                                        }
//                                        newUser.set(userMap);
                                        updateUI(user);
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.d(TAG, "onCreate: failure", task.getException());
                                        Toast.makeText(SignUpActivity.this, "Sign up failed. Email may be already in use!", Toast.LENGTH_SHORT).show();
                                        updateUI(null);
                                    }
                                }
                            });
                }
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        //hideProgressDialog();
        if (user != null) {
            //mStatusTextView.setText(getString(R.string.google_status_fmt, user.getEmail()));
            //mDetailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));

            //findViewById(R.id.signInButton).setVisibility(View.GONE);
            //findViewById(R.id.googleSignInButton).setVisibility(View.VISIBLE);

            Intent intent = new Intent(this, IntroActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            //mStatusTextView.setText(R.string.signed_out);
            //mDetailTextView.setText(null);
            //findViewById(R.id.signInButton).setVisibility(View.VISIBLE);
            //findViewById(R.id.signOutAndDisconnect).setVisibility(View.GONE);
        }
    }

}

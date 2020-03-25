package com.ufl.mybuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import com.google.ar.core.ArCoreApk;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import de.hdodenhof.circleimageview.CircleImageView;

import android.util.Log;

import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    // For SHA 1
    // https://stackoverflow.com/questions/15727912/sha-1-fingerprint-of-keystore-certificate

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    //What kind of account?
    private String mAccountType = "default";

    // AR Sceneform
    private ArFragment mArFragment;
    private ModelRenderable mCorgi;
    private boolean isModelPlaced = false;
    private Session mArSession;
    private Config mArConfig;
    private boolean mInstallRequested;
    private boolean mEnableAutoFocus;
    private boolean firstPlacement = false;

    private FloatingActionButton mVoiceFab;

    // Navigation Bar
    private NavigationView mNavigationView;
    private CircleImageView mNavProfileImage;
    private DrawerLayout mDrawerLayout;
    private TextView mProfileName;
    private ImageView mBtnDrawer;

    // Google Sign in
    private GoogleSignInClient mGoogleSignInClient;

    // Voice
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    private boolean mIsListening;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "In Main!");

        mInstallRequested = false;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();

        // Check if user is signed in (non-null) and update UI accordingly.
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not logged in, launch the Log In activity, double check herez
            loadLogInView();
        } else {
            // Load it up.
            init();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Initialize AR session if it does not exist
        if (mArSession == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !mInstallRequested)) {
                    case INSTALL_REQUESTED:
                        mInstallRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permission to operate.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                //Turn on auto focus mode
                //https://stackoverflow.com/questions/48913197/is-there-any-way-to-set-auto-focus-with-arcore-camera
                //Initialize session and configuration
                mArSession = new Session(this);
                mArConfig = new Config(mArSession);

                Log.d(TAG, "AR Config: " + mArConfig.getFocusMode());

                mEnableAutoFocus = true;

                if (mEnableAutoFocus) {
                    mArConfig.setFocusMode(Config.FocusMode.AUTO);
                }
                else {
                    mArConfig.setFocusMode(Config.FocusMode.FIXED);
                }

                //Sceneform requires that the ARCore session is configured to the UpdateMode LATEST_CAMERA_IMAGE.
                //This is probably not required for just auto focus. I was updating the camera configuration as well
                mArConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

                //Reconfigure the session
                mArSession.configure(mArConfig);

                //Setup the session with ARSceneView
                mArFragment.getArSceneView().setupSession(mArSession);

                Log.d(TAG, "AR Config: " + mArConfig.getFocusMode());

                //log out if the camera is in auto focus mode
                Log.d(TAG, "The camera is current in focus mode : ${config.focusMode.name}");
            }

            //Exception handling
            catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }
            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
    }

    // Function to load log in view if Firebase user is not logged in.
    private void loadLogInView() {
        Intent intent = new Intent(this, LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void init() {
        buttonInit();

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        //Automatically load pet model when plane detected on running app for first time
        //https://www.youtube.com/watch?v=ntEBeB37p5Q&list=PLsOU6EOcj51cEDYpCLK_bzo4qtjOwDWfW&index=18
        mArFragment.getArSceneView().getScene().addOnUpdateListener(this::onPlaneDetection);

        //TODO Food bowl
        // When tapping on the AR plane, the corgi will appear!
        // Option to tap model available after first reset
        mArFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (isModelPlaced) {
                        return;
                    }

                    if (mCorgi == null) {
                        return;
                    }
                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    loadPet(anchor);
//                    AnchorNode anchorNode = new AnchorNode(anchor);
//                    anchorNode.setParent(mArFragment.getArSceneView().getScene());
//
//                    // Create the transformable andy and add it to the anchor.
//                    TransformableNode andy = new TransformableNode(mArFragment.getTransformationSystem());
//                    andy.setParent(anchorNode);
//                    andy.setRenderable(mCorgi);
//                    andy.select();
                });

        drawerInit();

        voiceInit();

        // Find the account type logged in.
        for (UserInfo user : FirebaseAuth.getInstance().getCurrentUser().getProviderData()) {
            if (user.getProviderId().equals("facebook.com")) {
                //For linked FaceBook account
                Log.d(TAG, "User is signed in with Facebook");
                mAccountType = "facebook";
                break;
            } else if (user.getProviderId().equals("google.com")) {
                //For linked Google account
                Log.d(TAG, "User is signed in with Google");
                mAccountType = "google";
                break;
            }
        }
      
    }

    //Event listener triggered when plane detected in sceneform
    private void onPlaneDetection(FrameTime frameTime) {
        //Prevent model duplicates and account for first placement of the model
        if (isModelPlaced || firstPlacement) {
            return;
        }

        Frame frame = mArFragment.getArSceneView().getArFrame();

        Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);

        for (Plane plane : planes) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                //Place anchor and corresponding model at the plane center
                Anchor anchor = plane.createAnchor(plane.getCenterPose());

                //Load pet model
                loadPet(anchor);
                break;
            }
        }
    }

    //Function to load pet model created with blender
    private void loadPet(Anchor anchor) {
        isModelPlaced = true;
        firstPlacement = true;

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, Uri.parse("Corgi.sfb"))
                .build()
                .thenAccept(renderable -> {
                    mCorgi = renderable;
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    // Create the transformable pet and add it to the anchor.
                    TransformableNode corgi = new TransformableNode(mArFragment.getTransformationSystem());
                    corgi.setParent(anchorNode);
                    corgi.setRenderable(mCorgi);
                    corgi.select();
                })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Corgi", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }
    private void drawerInit() {
        mDrawerLayout = findViewById(R.id.drawable_layout);
        mNavigationView = findViewById(R.id.navigation_view);
        mBtnDrawer = findViewById(R.id.btn_menu);
        View navView = mNavigationView.inflateHeaderView(R.layout.navigation_header);
        mNavProfileImage = navView.findViewById(R.id.profile_image);

        mProfileName = navView.findViewById(R.id.profile_name);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference nameRef = db
                .collection("users")
                .document(mFirebaseUser.getUid());
        nameRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    String name = document.getData().get("name").toString();
                    Log.d(TAG, "Setting name: " + name);
                    mProfileName.setText(name);
                } else {
                    Log.d(TAG, "Couldn't get name. Probably shouldn't be in MainActivity");
                }
            }
        });


        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                UserMenuSelector(item);
                return false;
            }
        });
        mBtnDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });
    }

    private void voiceInit() {
        // set up the intent
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());

//        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
//        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 100);
//        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        SpeechRecognitionListener listener = new SpeechRecognitionListener();

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(listener);
    }

    boolean handleVoice(String command) {
        boolean matchFound = true;
        switch(command) {
            case "rollover":
                Toast.makeText(this, "Your pet is rolling over", Toast.LENGTH_SHORT).show();
                break;
            case "eat":
                Toast.makeText(this, "Your pet is eating", Toast.LENGTH_SHORT).show();
                break;
            case "play":
                Toast.makeText(this, "Your pet is playing", Toast.LENGTH_SHORT).show();
                break;
            case "sit":
                Toast.makeText(this, "Your pet is sitting", Toast.LENGTH_SHORT).show();
                break;
            default:
                matchFound = false;
        }

        return matchFound;
    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {
        @Override
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error)
        {
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

            Log.e(TAG, "error = " + error);
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        // continuously check partial results
        @Override
        public void onPartialResults(Bundle partialResults)
        {

        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "ready for speech");
//            speakButton.setImageResource(R.drawable.ic_stop);
            mIsListening = true;
        }

        // if there were no matches found, give error
        @Override
        public void onResults(Bundle results)
        {
//            speakButton.setImageResource(R.drawable.ic_mic);
            mIsListening = false;

            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            boolean matchFound = false;
            for (int i = 0; matches != null && i < matches.size() && !matchFound; i++) {
                for (String word : matches.get(i).split(" ")) {
                    if (!matchFound) {
                        matchFound = handleVoice(word);
                    }
                }
            }

            if (!matchFound) {
                Toast.makeText(MainActivity.this, "Command not recognized", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onRmsChanged(float rms)
        {

        }
    }

    private void buttonInit() {
        mVoiceFab = findViewById(R.id.voice);
        mVoiceFab.setOnClickListener(this);
        Button btn = findViewById(R.id.btnReset);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClear();
                isModelPlaced = false;

//                new Timer().schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        // this code will be executed after 2 seconds
//                        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
//                        mArFragment.getArSceneView().getScene().addOnUpdateListener(MainActivity.this::onPlaneDetection);
//                        isModelPlaced = false;
//                    }
//                }, 5000);
//                isModelPlaced = false;
//                firstPlacement = false;
//                mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
//                mArFragment.getArSceneView().getScene().addOnUpdateListener(MainActivity.this::onPlaneDetection);
            }
        });
    }





    private void UserMenuSelector(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.nav_home: {
                //Go HOME
                //Intent intent = new Intent(MainActivity.this, MainActivity.class);
                //startActivity(intent);
                //this.overridePendingTransition(0, 0);
                Toast.makeText(this, "Home Selected!", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.nav_settings: {
                //Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                //startActivity(intent);
                //SWITCH SETTINGS ACTIVITY
                Toast.makeText(this, "Settings Selected!", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.nav_logout: {
                //SIGN OUT
                signOut();
                break;
            }
        }

    }

    private void signOut() {
        // Firebase sign out
        mFirebaseAuth.signOut();

        // Google sign out
        if (mAccountType.equals("google")) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            mGoogleSignInClient.signOut().addOnCompleteListener(this,
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            loadLogInView();
                        }
                    });
//        } else if (mAccountType.equals("facebook")) {
//            LoginManager.getInstance().logOut();
//            loadLogInView();
        } else {
            loadLogInView();
        }
    }

    //Clear scene and anchors
    private void onClear() {
        List<Node> children = new ArrayList<>(mArFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                node.setParent(null);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.voice:
                if (!mIsListening)
                {
                    // check RECORD_AUDIO permission, https://developer.android.com/training/permissions/requesting#java
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted
                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.RECORD_AUDIO)) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Request to Record Audio")
                                    .setMessage("To use voice commands, MyBuddy needs to be able to record audio")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //Prompt the user once explanation has been shown
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                                    PERMISSION_REQUEST_RECORD_AUDIO);
                                        }
                                    })
                                    .create()
                                    .show();
                        } else {
                            // No explanation needed; request the permission
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    PERMISSION_REQUEST_RECORD_AUDIO);
                            onClick(view);
                        }
                    } else {
                        // Permission has already been granted
                        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                    }
                } else {
                    mSpeechRecognizer.cancel();
                }
                break;
            default:
                break;
        }
    }
}

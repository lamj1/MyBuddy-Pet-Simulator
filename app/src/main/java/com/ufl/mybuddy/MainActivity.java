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
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator.AnimatorListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
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
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SkeletonNode;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.AnimationData;
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
import com.squareup.picasso.Picasso;
import com.ufl.mybuddy.Intro.IntroActivity;
import com.ufl.mybuddy.Intro.TutorialActivity;

import org.ogasimli.healthbarview.HealthBarView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map;
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
    private boolean isBowlPlaced = false;
    private Session mArSession;
    private Config mArConfig;
    private boolean mInstallRequested;
    private boolean mEnableAutoFocus;
    private boolean firstPlacement = false;
    private Anchor anchor;
    private AnchorNode bowlAnchorNode;
    private TransformableNode corgi;
    private AnchorNode endLocation;
    private ModelRenderable mBowl;
    private SkeletonNode skeletonNode;
    private Node bowlNode;

    // AR Animation
    private ModelAnimator animateModel;
    private int count = 0;
    private ObjectAnimator animateObject;
    private Plane firstPlane;
    private float timer;
    private Pose pose;
    private boolean poseReceived = false;
    private boolean isAnimated = false;
    private boolean firstAnimation = false;
    Vector3 corgiPosition;

    // Foating Action Buttons users can tap on.
    private FloatingActionButton mVoiceFab;
    private FloatingActionButton mBowlFab;
    private FloatingActionButton mFoodBowlFab;
    private FloatingActionButton mWaterBowlFab;

    // Bowl has been set to not tapped yet.
    private boolean mRotate = false;

    // Hunger and Thirst Bars
    private HealthBarView mHungerBar;
    private HealthBarView mThirstBar;

    // Navigation Bar
    private NavigationView mNavigationView;
    private CircleImageView mNavProfileImage;
    private Uri mPhoto;
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
        mTimerHandler.removeCallbacks(updater);
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

        barInit();

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        //Automatically load pet model when plane detected on running app for first time
        //https://www.youtube.com/watch?v=ntEBeB37p5Q&list=PLsOU6EOcj51cEDYpCLK_bzo4qtjOwDWfW&index=18
        mArFragment.getArSceneView().getScene().addOnUpdateListener(this::onPlaneDetection);

        mArFragment.getArSceneView().getScene().addOnUpdateListener(this::onFrame);

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
                    anchor = hitResult.createAnchor();
                    loadPet(anchor);
                    firstAnimation = true;

                    if (corgi != null && firstAnimation) {
                        firstAnimation = false;
                        Log.d(TAG, "Corgi not null");
                        animate(mCorgi, "Armature|idle");
                    }
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
                anchor = plane.createAnchor(plane.getCenterPose());

                //Load pet model
                loadPet(anchor);
                firstAnimation = true;
                Log.d(TAG, "Pet loaded");

                break;
            }
        }
    }

    //Source: https://github.com/google-ar/sceneform-android-sdk/issues/490
    //Constantly generate a new location on the sceneform plane every 10 seconds
    private void onFrame(FrameTime frametime){
        // Keep track of the first valid plane detected, update it if the plane is lost or subsumed.

        if (firstPlane == null || firstPlane.getTrackingState() != TrackingState.TRACKING) {
            Collection<Plane> planes = mArFragment.getArSceneView().getSession()
                    .getAllTrackables(Plane.class);
            if (!planes.isEmpty()) {
                firstPlane = planes.iterator().next();
            }
            return;
        }
        if (firstPlane.getSubsumedBy() != null) {
            firstPlane = firstPlane.getSubsumedBy();
        }

        // Every 10 seconds get a new location on plane
        Log.d(TAG, "Timer: " + timer);
        if (timer > 10) {

            generateRandomPosition(firstPlane);
            timer = 0;
        }
        timer += frametime.getDeltaSeconds();

        //Create anchor at random location on plane
        if (isModelPlaced && poseReceived) {
            Log.d(TAG, "Random anchor created");
            poseReceived = false;
            Anchor randAnchor = mArFragment.getArSceneView().getSession().createAnchor(pose);
            endLocation = new AnchorNode(randAnchor);
            endLocation.setParent(mArFragment.getArSceneView().getScene());


//            Log.d(TAG, "Animate walk");
//            animateWalk();
        }

        //Animate idle when first model placed
        if (corgi != null && firstAnimation) {
            firstAnimation = false;
            Log.d(TAG, "Corgi not null");
            animate(mCorgi, "Armature|idle");
        }

        if (corgi != null) {
            corgiPosition = corgi.getForward();
        }
    }

    //Generate random positions on plane for anchor
    private static Random rand = new Random();

    private void generateRandomPosition(Plane plane) {
        //Generate random location on plane
        float maxX = plane.getExtentX() * 2;
        float randomX = (maxX * rand.nextFloat()) - plane.getExtentX();

        float maxZ = plane.getExtentZ() * 2;
        float randomZ = (maxZ * rand.nextFloat()) - plane.getExtentZ();

        pose = plane.getCenterPose();
        float[] translation = pose.getTranslation();
        float[] rotation = pose.getRotationQuaternion();

        translation[0] += randomX;
        translation[2] += randomZ;
        pose = new Pose(translation, rotation);
        poseReceived = true;
        Log.d(TAG, "Pose received");
    }

    //Function to load pet model created with blender
    private void loadPet(Anchor anchor) {
        isModelPlaced = true;
        firstPlacement = true;

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, Uri.parse("Corgi_Combined2.sfb"))
                .build()
                .thenAccept(renderable -> {
                    mCorgi = renderable;
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    skeletonNode = new SkeletonNode();
                    skeletonNode.setRenderable(mCorgi);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    // Create the transformable pet and add it to the anchor.
                    // Add child skeleton node to transformable node
                    corgi = new TransformableNode(mArFragment.getTransformationSystem());
                    corgi.setParent(anchorNode);
                    corgi.addChild(skeletonNode);
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

    //Function to load bowl model created with blender
    private void loadBowl(Anchor anchor) {
        isBowlPlaced = true;

        corgiPosition = corgi.getForward();
        Vector3 bowlPosition = corgi.worldToLocalDirection(corgiPosition).scaled(0.7f);

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, Uri.parse("Corgi_bowl_thick.sfb"))
                .build()
                .thenAccept(renderable -> {
                    mBowl = renderable;
                    bowlAnchorNode = new AnchorNode(anchor);
                    bowlAnchorNode.setParent(corgi);

                    bowlNode = new Node();
                    bowlNode.setLocalPosition(bowlPosition);
                    bowlNode.setParent(bowlAnchorNode);
                    bowlNode.setRenderable(mBowl);

                })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Bowl", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    // Run animation on model
    private void animate(ModelRenderable renderable, String command) {
        AnimatorListenerAdapter listenerAdapter;
        if (isModelPlaced == false){
            Toast.makeText(this, "Pet model not placed", Toast.LENGTH_SHORT).show();
            return;
        }

        isAnimated = true;

        // End any running animation
        if (animateModel != null && animateModel.isRunning()) {
            animateModel.end();
        }

        int animationCount = renderable.getAnimationDataCount();

        if (count == animationCount) {
            count = 0;
        }

        AnimationData animationData = renderable.getAnimationData(command);

        animateModel = new ModelAnimator(animationData, renderable);

        // Set default idle animation to loop
        if (command.equals("Armature|idle")) {
            animateModel.setRepeatCount(Animation.INFINITE);
            Log.d(TAG, "Idle animation");
        }

        animateModel.start();
        count++;

        if (!command.equals("Armature|idle")) {
            //Animate idle when placed
            listenerAdapter = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (mCorgi != null) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                //Clear bowl if eating animated
                                if (isBowlPlaced) {
                                    isBowlPlaced = false;
                                    clearBowl();
                                }

                                Log.d(TAG, "Animation ended");
                                animate(mCorgi, "Armature|idle");
                            }
                        }, 4500);   //4.5 second delay to allow current animation to complete
                    }
                }
            };
            listenerAdapter.onAnimationEnd(animateModel);
        }
    }

    //Walk animation
    private void animateWalk() {
        //Avoid walking when no model is present or mid animation
        if (isModelPlaced == false || isAnimated) {
            return;
        }
        animateObject = new ObjectAnimator();
        animateObject.setAutoCancel(true);
        animateObject.setTarget(corgi);

        // All the positions should be world positions
        // The first position is the start, and the second is the end.
        animateObject.setObjectValues(corgi.getWorldPosition(), endLocation.getWorldPosition());

        // Use setWorldPosition to position corgi.
        animateObject.setPropertyName("worldPosition");

        animateObject.setEvaluator(new Vector3Evaluator());

        // Linear animation
        animateObject.setInterpolator(new LinearInterpolator());

        // Set animation to duration of 5 seconds
        animateObject.setDuration(5000);
        animateObject.start();
}

    private void drawerInit() {
        mDrawerLayout = findViewById(R.id.drawable_layout);
        mNavigationView = findViewById(R.id.navigation_view);
        mBtnDrawer = findViewById(R.id.btn_menu);
        View navView = mNavigationView.inflateHeaderView(R.layout.navigation_header);
        mNavProfileImage = navView.findViewById(R.id.profile_image);
        mPhoto = mFirebaseUser.getPhotoUrl();
        Picasso.with(MainActivity.this).load(mPhoto.toString()).placeholder(R.drawable.doggo).into(mNavProfileImage);

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

        SpeechRecognitionListener listener = new SpeechRecognitionListener();

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(listener);
    }

    boolean handleVoice(String command) {
        boolean matchFound = true;
        switch(command) {
            case "laydown":
                animate(mCorgi, "Armature|layDown");
                Toast.makeText(this, "Your buddy is laying down", Toast.LENGTH_SHORT).show();
                isAnimated = false;
                break;
            case "rollover":
                animate(mCorgi, "Armature|rollOver");
                Toast.makeText(this, "Your buddy is rolling over", Toast.LENGTH_SHORT).show();
                isAnimated = false;
                break;
            case "play":
                animate(mCorgi, "Armature|Play");
                Toast.makeText(this, "Your buddy is playing", Toast.LENGTH_SHORT).show();
                break;
            case "jump":
                animate(mCorgi, "Armature|jump");
                Toast.makeText(this, "Your buddy is jumping", Toast.LENGTH_SHORT).show();
                isAnimated = false;
                break;
            case "bow":
                animate(mCorgi, "Armature|bow");
                Toast.makeText(this, "Your buddy is bowing", Toast.LENGTH_SHORT).show();
                isAnimated = false;
                break;
            case "sit":
                animate(mCorgi, "Armature|sit");
                Toast.makeText(this, "Your buddy is sitting", Toast.LENGTH_SHORT).show();
                isAnimated = false;
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
            Log.d(TAG, "end of speech");
            mVoiceFab.setImageResource(R.drawable.ic_voice_red_24dp);
            mIsListening = false;
        }

        @Override
        public void onError(int error)
        {
            Log.e(TAG, "voice error = " + error);
            mSpeechRecognizer.cancel();
            this.onEndOfSpeech();
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
            mVoiceFab.setImageResource(R.drawable.ic_voice_gray_24dp);
            mIsListening = true;
        }

        // if there were no matches found, give error
        @Override
        public void onResults(Bundle results)
        {
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

        mBowlFab = findViewById(R.id.bowl);
        mFoodBowlFab = findViewById(R.id.bowl_food);
        mWaterBowlFab = findViewById(R.id.bowl_water);


        // Setting bowls to be invisible at the start
        mFoodBowlFab.setVisibility(View.GONE);
        mFoodBowlFab.setTranslationY(mFoodBowlFab.getHeight());
        mFoodBowlFab.setAlpha(0f);
        mWaterBowlFab.setVisibility(View.GONE);
        mWaterBowlFab.setTranslationY(mWaterBowlFab.getHeight());
        mWaterBowlFab.setAlpha(0f);



        mBowlFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrCloseBowls();
            }
        });

        mFoodBowlFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrCloseBowls();
                loadBowl(anchor);
                animate(mCorgi, "Armature|eat");
                isAnimated = false;
                int hunger = (int) mHungerBar.getValue();
                hunger = Math.max(90, Math.min(hunger + 10, 100));
                updateBar("hunger", hunger);
                Toast.makeText(MainActivity.this, "Feeding Buddy!", Toast.LENGTH_SHORT).show();
            }
        });

        mWaterBowlFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrCloseBowls();
                loadBowl(anchor);
                animate(mCorgi, "Armature|eat");
                isAnimated = false;
                int thirst = (int) mThirstBar.getValue();
                thirst = Math.max(90, Math.min(thirst + 10, 100));
                updateBar("thirst", thirst);
                Toast.makeText(MainActivity.this, "Giving Water to Buddy!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openOrCloseBowls() {
        mRotate = rotateFab(mBowlFab, !mRotate);
        if(mRotate) {
            showIn(mFoodBowlFab);
            showIn(mWaterBowlFab);
        } else{
            showOut(mFoodBowlFab);
            showOut(mWaterBowlFab);
        }
    }

    private boolean rotateFab(final View v, boolean rotate) {
        v.animate().setDuration(300)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                }
            })
            .rotation(rotate ? 360f : 0f);

        return rotate;
    }

    private void showIn(final View v) {
        v.setVisibility(View.VISIBLE);
        v.setAlpha(0f);
        v.setTranslationY(v.getHeight());
        v.animate()
                .setDuration(300)
                .translationY(0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                })
                .alpha(1f)
                .start();
    }
    private void showOut(final View v) {
        v.setVisibility(View.VISIBLE);
        v.setAlpha(1f);
        v.setTranslationY(0);
        v.animate()
                .setDuration(300)
                .translationY(v.getHeight())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v.setVisibility(View.GONE);
                        super.onAnimationEnd(animation);
                    }
                }).alpha(0f)
                .start();
    }

    //Either update the "hunger" or "thirst" value
    private void updateBar(String type, int value) {
        if (type.equals("hunger") || type.equals("thirst")) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db
                    .collection("users")
                    .document(mFirebaseUser.getUid());
            Map<String, Object> values = new HashMap<>();

            if (type.equals("hunger")) {
                mHungerBar.setValue(value);
                values.put("hungerValue", value);
            } else if (type.equals("thirst")) {
                mThirstBar.setValue(value);
                values.put("thirstValue", value);
            }

            userRef.update(values);
        }
    }

    // Initializes the Hunger and Thirst Bars.
    // https://stackoverflow.com/questions/40103742/update-textview-every-second-in-android
    Runnable updater;
    final Handler mTimerHandler = new Handler();
    //1000 is 1 second. 60,000 is 1 minute. 30,000 is 30 seconds
    private int time = 60000;

    // Uses https://github.com/ogasimli/HealthBarView
    private void barInit() {
        mHungerBar = findViewById(R.id.hunger_bar);
        mThirstBar = findViewById(R.id.thirst_bar);

        updater = new Runnable() {
            @Override
            public void run() {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference userRef = db
                        .collection("users")
                        .document(mFirebaseUser.getUid());
                userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            int hungerValue = document.getLong("hungerValue").intValue();
                            int thirstValue = document.getLong("thirstValue").intValue();
                            if (hungerValue == 0 && thirstValue == 0) {
                                mHungerBar.setValue(hungerValue);
                                mThirstBar.setValue(thirstValue);
                                Log.d(TAG, "hunger and thirst are 0.");
                            } else {
                                hungerValue = Math.max(hungerValue - 1, 0);
                                thirstValue = Math.max(thirstValue - 2, 0);
                                mHungerBar.setValue(hungerValue);
                                mThirstBar.setValue(thirstValue);
                                Map<String, Object> values = new HashMap<>();
                                //values.put("name", document.getData().get("name").toString());
                                values.put("hungerValue", hungerValue);
                                values.put("thirstValue", thirstValue);
                                userRef.update(values);
                                Log.d(TAG, "Got and Setting hunger and thirst as: hunger: " + hungerValue + " thirst: " + thirstValue);
                            }
                        } else {
                            Log.d(TAG, "Couldn't get hunger and thirst values.");
                        }
                    }
                });

                mTimerHandler.postDelayed(updater,time);
            }
        };
        mTimerHandler.post(updater);
    }

    private void UserMenuSelector(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.nav_home: {
                //Go HOME
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                //this.overridePendingTransition(0, 0);
                //Toast.makeText(this, "Home Selected!", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.nav_stats: {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                startActivity(intent);
            }

            case R.id.nav_reset: {
                onClear();
                isModelPlaced = false;
                break;
            }

            case R.id.nav_settings: {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                //SWITCH SETTINGS ACTIVITY
                //Toast.makeText(this, "Settings Selected!", Toast.LENGTH_SHORT).show();
                break;
            }

            case R.id.nav_tutorial: {
                Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                startActivity(intent);
                //SWITCH Tutorial / Intro ACTIVITY
                //Toast.makeText(this, "Tutorial Selected!", Toast.LENGTH_SHORT).show();
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

    // Clear the bowl from the scene
    private void clearBowl() {
        if (bowlAnchorNode.getAnchor() != null) {
            bowlNode.getParent().removeChild(bowlNode);
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
                    mSpeechRecognizer.stopListening();
                }
                break;
            default:
                break;
        }
    }
}

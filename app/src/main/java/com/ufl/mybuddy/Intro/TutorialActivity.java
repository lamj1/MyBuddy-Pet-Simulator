package com.ufl.mybuddy.Intro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ufl.mybuddy.MainActivity;
import com.ufl.mybuddy.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager mScreenPager;
    private IntroViewPagerAdapter mIntroViewPagerAdapter;
    private TabLayout mTabIndicator;
    private Button mBtnNext;
    private int mPagePosition = 0;
    private Button mBtnGetStarted;
    private Animation mBtnAnim;
    private TextView mSkip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make the activity on full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_tutorial);

        // Rid the action bar
        getSupportActionBar().hide();

        // initialize views
        mBtnNext = findViewById(R.id.btn_next);
        mBtnGetStarted = findViewById(R.id.btn_get_started);
        mTabIndicator = findViewById(R.id.tab_indicator);
        mBtnAnim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.button_animation);
        mSkip = findViewById(R.id.tv_skip);

        // Fill pages
        final List<ScreenItem> mList = new ArrayList<>();
        mList.add(new ScreenItem("Take Care of Your Buddy","Learn how to raise a pet of your own! Interact with your buddy in various environments such as your living room or outside at the park.\n\n(Check out the settings to change their picture and name!)", R.drawable.introimg1));
        mList.add(new ScreenItem("Feed Your Buddy","Use the water and food bowls to feed your buddy throughout the day. Just like real pets, your buddy gets hungry and thirsty too!\n\n(Check out the stats page to see stats like this!)",R.drawable.introimg2));
        mList.add(new ScreenItem("Teach Your Buddy Commands","Tap on the microphone button and repeat any of the following commands:\n\"Sit, Jump, Eat, Roll Over, Lay Down, Bow, Play\"\nYour Buddy should follow your voice and do as you say!",R.drawable.introimg3));

        // Viewpager
        mScreenPager = findViewById(R.id.screen_viewpager);
        mIntroViewPagerAdapter = new IntroViewPagerAdapter(this, mList);
        mScreenPager.setAdapter(mIntroViewPagerAdapter);

        // Tablayout with viewpager

        mTabIndicator.setupWithViewPager(mScreenPager);

        // Next button with OnClickListner

        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPagePosition = mScreenPager.getCurrentItem();
                if (mPagePosition < mList.size()) {
                    mPagePosition++;
                    mScreenPager.setCurrentItem(mPagePosition);
                }

                if (mPagePosition == mList.size()-1) { // when we rech to the last screen
                    loaddLastScreen();
                }
            }
        });


        mTabIndicator.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == mList.size()-1) {
                    loaddLastScreen();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });



        // Get Started button click listener

        mBtnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open welcome activity
                Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainActivity);
                finish();
            }
        });

        // skip button click listener

        mSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mScreenPager.setCurrentItem(mList.size());
            }
        });

    }

    // show the GETSTARTED Button and hide the indicator and the next button
    private void loaddLastScreen() {

        mBtnNext.setVisibility(View.INVISIBLE);
        mBtnGetStarted.setVisibility(View.VISIBLE);
        mSkip.setVisibility(View.INVISIBLE);
        mTabIndicator.setVisibility(View.INVISIBLE);

        mBtnGetStarted.setAnimation(mBtnAnim);

    }
}
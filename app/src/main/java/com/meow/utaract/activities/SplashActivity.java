package com.meow.utaract.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.meow.utaract.R;
import com.meow.utaract.firebase.AuthService;
import com.meow.utaract.utils.GuestProfileStorage;

/**
 * SplashActivity shows the splash screen with animations before
 * navigating to the appropriate next activity.
 *
 * Features:
 * - Logo scale & glow animation
 * - Ripple animation
 * - Typewriter effect for slogan
 * - User state check (organiser, guest, returning guest, or new user)
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3400; // Total splash duration (ms)
    private static final int TYPEWRITER_DELAY = 30; // Delay between each character in slogan
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000; // (Unused constant, reserved for Google Play Services)

    private ImageView ivLogo;
    private TextView tvSlogan;
    private View rippleView;
    private CharSequence sloganText;
    private int sloganIndex;
    private final Handler typewriterHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator glowAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        ivLogo = findViewById(R.id.iv_logo);
        tvSlogan = findViewById(R.id.tv_slogan);
        rippleView = findViewById(R.id.ripple_view);

        // Set initial visibility
        ivLogo.setVisibility(View.INVISIBLE);
        rippleView.setVisibility(View.INVISIBLE);

        // Setup slogan text and clear existing
        sloganText = "Activity . Community . Togetherness";
        tvSlogan.setText("");

        // Prepare glow animation and start entry animations
        setupGlowAnimator();
        startAnimations();
    }

    /**
     * Sets up the glow effect animator for the logo.
     * The logo will fade from a white glow to transparent.
     */
    private void setupGlowAnimator() {
        int startColor = Color.parseColor("#A0FFFFFF"); // Semi-transparent white
        int endColor = Color.TRANSPARENT;

        glowAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        glowAnimator.setDuration(1100); // Glow duration

        // Apply animated color as a filter to the logo
        glowAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            ivLogo.setColorFilter(animatedValue, PorterDuff.Mode.SRC_ATOP);
        });
    }

    /**
     * Starts the splash screen animations:
     * - Scale in logo
     * - Ripple background
     * - Glow effect
     * - Typewriter slogan
     * After animations, transitions to next activity.
     */
    private void startAnimations() {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        Animation ripple = AnimationUtils.loadAnimation(this, R.anim.ripple_effect);
        Animation zoomInOut = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);

        // Ripple hides itself after finishing
        ripple.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                rippleView.setVisibility(View.GONE);
            }
            @Override public void onAnimationRepeat(Animation animation) {}
        });

        // Scale-in triggers ripple + glow + typewriter effect
        scaleIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                rippleView.setVisibility(View.VISIBLE);
                rippleView.startAnimation(ripple);
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                ivLogo.startAnimation(zoomInOut);
                glowAnimator.start();
                typewriterHandler.postDelayed(characterAdder, TYPEWRITER_DELAY);
            }
            @Override public void onAnimationRepeat(Animation animation) {}
        });

        // Start logo animation
        ivLogo.setVisibility(View.VISIBLE);
        ivLogo.startAnimation(scaleIn);

        // Transition to next activity after splash delay
        new Handler(Looper.getMainLooper()).postDelayed(this::nextActivity, SPLASH_DELAY);
    }

    /**
     * Runnable that types out the slogan text character by character
     * using the typewriter effect.
     */
    private final Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            if (sloganIndex <= sloganText.length()) {
                tvSlogan.setText(sloganText.subSequence(0, sloganIndex++));
                typewriterHandler.postDelayed(this, TYPEWRITER_DELAY);
            }
        }
    };

    /**
     * Decides which activity to launch after the splash screen based on user state:
     * - Signed in & verified organiser → MainActivity (organiser mode)
     * - Signed in anonymously → MainActivity (guest mode)
     * - Local guest profile exists → MainActivity (guest mode)
     * - Otherwise → LoginActivity
     */
    private void nextActivity() {
        FirebaseUser currentUser = new AuthService().getAuth().getCurrentUser();
        GuestProfileStorage profileStorage = new GuestProfileStorage(this);

        if (currentUser != null && !currentUser.isAnonymous() && currentUser.isEmailVerified()) {
            // Case 1: Verified organiser
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", true);
            startActivity(intent);
        } else if (currentUser != null && currentUser.isAnonymous()) {
            // Case 2: Anonymous guest
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", false);
            startActivity(intent);
        } else if (profileStorage.profileExists()) {
            // Case 3: Returning guest (local profile only)
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", false);
            startActivity(intent);
        } else {
            // Case 4: New user → login
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up: remove handlers & stop animations to prevent memory leaks
        typewriterHandler.removeCallbacks(characterAdder);
        ivLogo.clearAnimation();
        rippleView.clearAnimation();
        if (glowAnimator != null && glowAnimator.isRunning()) {
            glowAnimator.cancel();
        }
    }
}

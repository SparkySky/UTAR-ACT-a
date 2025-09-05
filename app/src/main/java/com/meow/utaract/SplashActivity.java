package com.meow.utaract;

import android.animation.ArgbEvaluator; // Import for color interpolation
import android.animation.ValueAnimator; // Import for value animation
import android.content.Intent;
import android.graphics.Color; // Import for Color class
import android.graphics.PorterDuff; // Import for PorterDuff
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // For getting colors reliably

import com.google.firebase.auth.FirebaseUser;
import com.meow.utaract.firebase.AuthService;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3500;
    private static final int TYPEWRITER_DELAY = 36;

    private ImageView ivLogo;
    private TextView tvSlogan;
    private View rippleView;
    private CharSequence sloganText;
    private int sloganIndex;
    private final Handler typewriterHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator glowAnimator; // Declare the ValueAnimator

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ivLogo = findViewById(R.id.iv_logo);
        tvSlogan = findViewById(R.id.tv_slogan);
        rippleView = findViewById(R.id.ripple_view);

        ivLogo.setVisibility(View.INVISIBLE);
        rippleView.setVisibility(View.INVISIBLE);

        sloganText = "Activity . Community . Togetherness";
        tvSlogan.setText("");

        setupGlowAnimator(); // Initialize the glow animator
        startAnimations();
    }

    private void setupGlowAnimator() {
        // --- THIS IS THE FIX ---
        // Animate from a bright white overlay to fully transparent (normal)
        int startColor = Color.parseColor("#A0FFFFFF"); // A semi-transparent white for the "bright" effect
        int endColor = Color.TRANSPARENT; // Fades to no overlay

        glowAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        glowAnimator.setDuration(1000); // Duration of the fade

        // This animation will only run once, not repeat.
        glowAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            ivLogo.setColorFilter(animatedValue, PorterDuff.Mode.SRC_ATOP);
        });
    }

    private void startAnimations() {
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        Animation ripple = AnimationUtils.loadAnimation(this, R.anim.ripple_effect);
        Animation zoomInOut = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);

        ripple.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                rippleView.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        scaleIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                rippleView.setVisibility(View.VISIBLE);
                rippleView.startAnimation(ripple);
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                // Start continuous zoom and the new brightening effect
                ivLogo.startAnimation(zoomInOut);
                glowAnimator.start(); // Start the glow animator
                typewriterHandler.postDelayed(characterAdder, TYPEWRITER_DELAY);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        ivLogo.setVisibility(View.VISIBLE);
        ivLogo.startAnimation(scaleIn);

        new Handler(Looper.getMainLooper()).postDelayed(this::nextActivity, SPLASH_DELAY);
    }

    private final Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            if (sloganIndex <= sloganText.length()) {
                tvSlogan.setText(sloganText.subSequence(0, sloganIndex++));
                typewriterHandler.postDelayed(this, TYPEWRITER_DELAY);
            }
        }
    };

    private void nextActivity() {
        FirebaseUser currentUser = new AuthService().getAuth().getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous() && currentUser.isEmailVerified()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", true);
            startActivity(intent);
        } else if (currentUser != null && currentUser.isAnonymous()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", false);
            startActivity(intent);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        typewriterHandler.removeCallbacks(characterAdder);
        ivLogo.clearAnimation();
        rippleView.clearAnimation();
        if (glowAnimator != null && glowAnimator.isRunning()) {
            glowAnimator.cancel(); // Cancel the glow animator
        }
    }
}
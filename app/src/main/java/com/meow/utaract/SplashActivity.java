package com.meow.utaract;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView ivLogo;
    private TextView tvAppName, tvTagline;
    private ProgressBar progressBar;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        ivLogo = findViewById(R.id.iv_logo);
        tvAppName = findViewById(R.id.tv_app_name);
        tvTagline = findViewById(R.id.tv_tagline);
        progressBar = findViewById(R.id.progress_bar);

        handler = new Handler(Looper.getMainLooper());

        // Start animations
        startAnimations();
    }

    private void startAnimations() {
        // Logo animation - scale, rotate, and bounce
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation);
        ivLogo.startAnimation(logoAnimation);

        // App name animation - fade in with delay
        handler.postDelayed(() -> {
            Animation appNameAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            tvAppName.startAnimation(appNameAnimation);
            tvAppName.animate().alpha(1.0f).setDuration(1000).start();
        }, 800);

        // Tagline animation - slide up with delay
        handler.postDelayed(() -> {
            Animation taglineAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            tvTagline.startAnimation(taglineAnimation);
            tvTagline.animate().alpha(1.0f).setDuration(600).start();
        }, 1200);

        // Progress bar pulse animation
        handler.postDelayed(() -> {
            Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
            progressBar.startAnimation(pulseAnimation);
        }, 1500);

        // Navigate to LoginActivity after animations complete
        handler.postDelayed(this::navigateToLogin, 3000);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
        
        // Add fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
} 
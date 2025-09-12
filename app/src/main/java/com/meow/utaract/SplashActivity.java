package com.meow.utaract;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseUser;
import com.meow.utaract.firebase.AuthService;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3400;
    private static final int TYPEWRITER_DELAY = 30;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

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

        ivLogo = findViewById(R.id.iv_logo);
        tvSlogan = findViewById(R.id.tv_slogan);
        rippleView = findViewById(R.id.ripple_view);
        ivLogo.setVisibility(View.INVISIBLE);
        rippleView.setVisibility(View.INVISIBLE);
        sloganText = "Activity . Community . Togetherness";
        tvSlogan.setText("");

        setupGlowAnimator();
        startAnimations();
    }

    private void setupGlowAnimator() {
        int startColor = Color.parseColor("#A0FFFFFF");
        int endColor = Color.TRANSPARENT;

        glowAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        glowAnimator.setDuration(1100);

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
                ivLogo.startAnimation(zoomInOut);
                glowAnimator.start();
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
            glowAnimator.cancel();
        }
    }
}
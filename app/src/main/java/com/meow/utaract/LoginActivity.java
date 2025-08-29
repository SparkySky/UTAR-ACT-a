package com.meow.utaract;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseUser;
import com.meow.utaract.firebase.AuthService;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, guestButton;
    private TextView signupTextLink, verificationStatusText;
    private ImageButton themeToggleButton;

    public static final String EXTRA_EMAIL_FOR_VERIFICATION_CHECK = "email_for_verification_check";
    public static final String ACTION_EMAIL_VERIFIED = "com.meow.utaract.EMAIL_VERIFIED";
    private static final int SIGNUP_REQUEST_CODE = 101;
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "SelectedTheme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_login);

        initializeViews();
        setupListeners();
        handleIntent(getIntent());
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInputLogin);
        passwordInput = findViewById(R.id.passwordInputLogin);
        loginButton = findViewById(R.id.loginButton);
        guestButton = findViewById(R.id.guestButton);
        signupTextLink = findViewById(R.id.signupTextLink);
        verificationStatusText = findViewById(R.id.verificationStatusText);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        updateToggleIcon();
    }

    private void setupListeners() {
        themeToggleButton.setOnClickListener(v -> toggleTheme());
        loginButton.setOnClickListener(v -> loginUser());
        guestButton.setOnClickListener(v -> continueAsGuest());
        signupTextLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivityForResult(intent, SIGNUP_REQUEST_CODE);
        });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();


        if (!isFormValid(email, password)) {
            return;
        }
        setButtonsEnabled(false);
        verificationStatusText.setVisibility(View.GONE);

        new AuthService().getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = new AuthService().getAuth().getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("IS_ORGANISER", true);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                            setButtonsEnabled(true);
                            if (user != null) {
                                user.sendEmailVerification();
                            }
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void continueAsGuest() {
        verificationStatusText.setVisibility(View.GONE);
        setButtonsEnabled(false);
        new AuthService().getAuth().signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Continuing as Guest.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("IS_ORGANISER", false);
                        startActivity(intent);
                        finish();
                    } else {
                        setButtonsEnabled(true);
                        Toast.makeText(LoginActivity.this, "Guest login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isFormValid(String email, String password) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }
        return true;
    }

    private void toggleTheme() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            saveThemePreference("light");
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            saveThemePreference("dark");
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedTheme = prefs.getString(KEY_THEME, "system"); // Default to system
        switch (savedTheme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void saveThemePreference(String themeValue) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_THEME, themeValue);
        editor.apply();
    }

    private void updateToggleIcon() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeToggleButton.setImageResource(R.drawable.ic_sun);
            themeToggleButton.setContentDescription("Switch to Light Mode");
        } else {
            themeToggleButton.setImageResource(R.drawable.ic_moon);
            themeToggleButton.setContentDescription("Switch to Dark Mode");
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateToggleIcon();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && ACTION_EMAIL_VERIFIED.equals(intent.getAction())) {
            String email = intent.getStringExtra(EXTRA_EMAIL_FOR_VERIFICATION_CHECK);
            if (email != null) {
                emailInput.setText(email);
                passwordInput.requestFocus();
            }
            verificationStatusText.setText("Email verified successfully! Please log in.");
            verificationStatusText.setVisibility(View.VISIBLE);
            intent.setAction(null);
        } else if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null && data.toString().contains("utaract.page.link/verify")) {
                // Logic to handle user returning from email verification link
                String emailFromLink = data.getQueryParameter("email");
                if (emailFromLink != null) {
                    emailInput.setText(emailFromLink);
                    verificationStatusText.setText("Please enter your password to complete login.");
                    verificationStatusText.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGNUP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String email = data.getStringExtra(EXTRA_EMAIL_FOR_VERIFICATION_CHECK);
            if (email != null) {
                emailInput.setText(email);
                verificationStatusText.setText("Verification email sent to " + email + ". Please check your inbox, then log in.");
                verificationStatusText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = new AuthService().getAuth().getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", true);
            startActivity(intent);
            finish();
        }
    }

    // Always re-enable the buttons when the activity comes into focus.
    // This handles cases where the user logs out and returns to this screen.
    @Override
    protected void onResume() {
        super.onResume();
        setButtonsEnabled(true);
    }


    // Helper method to control the state of both buttons (Login and Guest)
    private void setButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        loginButton.setClickable(enabled);
        guestButton.setEnabled(enabled);
        guestButton.setClickable(enabled);
    }
}
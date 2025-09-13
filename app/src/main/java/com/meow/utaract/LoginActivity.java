package com.meow.utaract;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.meow.utaract.firebase.AuthService;
import com.meow.utaract.utils.GuestProfileStorage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, guestButton, resendEmailButton;
    private TextView signupTextLink, verificationStatusText, forgotPasswordLink;
    private ImageButton themeToggleButton;
    private CountDownTimer resendTimer;
    private long resendCooldownEndTime = 0;

    public static final String EXTRA_EMAIL_FOR_VERIFICATION_CHECK = "email_for_verification_check";
    public static final String ACTION_EMAIL_VERIFIED = "com.meow.utaract.EMAIL_VERIFIED";
    private static final int SIGNUP_REQUEST_CODE = 101;
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "SelectedTheme";

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_login);

        initializeViews();
        auth = FirebaseAuth.getInstance();
        setupListeners();
        handleIntent(getIntent());
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInputLogin);
        passwordInput = findViewById(R.id.passwordInputLogin);
        loginButton = findViewById(R.id.loginButton);
        guestButton = findViewById(R.id.guestButton);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        signupTextLink = findViewById(R.id.signupTextLink);
        verificationStatusText = findViewById(R.id.verificationStatusText);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        resendEmailButton = findViewById(R.id.resendEmailButton);
        updateToggleIcon();
    }

    private void setupListeners() {
        themeToggleButton.setOnClickListener(v -> toggleTheme());
        loginButton.setOnClickListener(v -> loginUser());
        guestButton.setOnClickListener(v -> continueAsGuest());
        forgotPasswordLink.setOnClickListener(v -> resetPassword());
        signupTextLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivityForResult(intent, SIGNUP_REQUEST_CODE);
        });
        resendEmailButton.setOnClickListener(v -> {
            FirebaseUser user = new AuthService().getAuth().getCurrentUser();
            if (user != null && !user.isEmailVerified()) {
                user.sendEmailVerification().addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                    startResendCooldown(60000); // Start 60-second cooldown
                }).addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to send email.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!isFormValid(email, password)) return;
        setButtonsEnabled(false);

        // Hide verification views on new login attempt
        setButtonsEnabled(false);
        verificationStatusText.setVisibility(View.GONE);
        resendEmailButton.setVisibility(View.GONE);
        setLoginButtonWeight(1.0f);

        boolean isOrganiser = isOrganiserEmail(email);

        if (email.endsWith("@utar.my") == false && email.endsWith("@1utar.my") == false && isOrganiser) {
            emailInput.setError("Organiser must use @utar.my or @1utar.my email");
            emailInput.requestFocus();
            return;
        }

        setButtonsEnabled(false);
        verificationStatusText.setVisibility(View.GONE);
        
        new AuthService().getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setButtonsEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = new AuthService().getAuth().getCurrentUser();
                        getAndStoreFcmToken(user.getUid());
                        if (user != null && user.isEmailVerified()) {
                            // Start MainActivity immediately after successful login
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("IS_ORGANISER", true);
                            startActivity(intent);
                            finish(); // Finish LoginActivity so user can't go back to it
                        } else {
                            // User is not verified
                            verificationStatusText.setText("Please verify your email to continue.");
                            verificationStatusText.setVisibility(View.VISIBLE);
                            resendEmailButton.setVisibility(View.VISIBLE);
                            // Adjust button weights to show both
                            setLoginButtonWeight(0.5f);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isOrganiserEmail(String email) {
        return email.endsWith("@utar.my") || email.endsWith("@1utar.my");
    }
    private void continueAsGuest() {
        new AuthService().getAuth().signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Check if a local profile already exists
                        GuestProfileStorage profileStorage = new GuestProfileStorage(LoginActivity.this);
                        if (profileStorage.profileExists()) {
                            // Profile exists, go to main activity
                            Toast.makeText(LoginActivity.this, "Continuing as Guest.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("IS_ORGANISER", false);
                            startActivity(intent);
                            finish();
                        } else {
                            // No profile exists, go to guest form to create one
                            Toast.makeText(LoginActivity.this, "Please set up your guest profile.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, GuestFormActivity.class);
                            intent.putExtra("IS_ORGANISER", false);
                            startActivity(intent);
                            finish();
                        }
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
        // This part is still useful for users who are ALREADY logged in when they open the app.
        FirebaseUser currentUser = new AuthService().getAuth().getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous() && currentUser.isEmailVerified()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", true);
            startActivity(intent);
            finish();
        } else if (currentUser != null && currentUser.isAnonymous()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", false);
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
        // If a cooldown was active, resume it
        long timeLeft = resendCooldownEndTime - System.currentTimeMillis();
        if (timeLeft > 0) {
            startResendCooldown(timeLeft);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (resendTimer != null) {
            resendTimer.cancel(); // Stop the timer when the activity is not visible
        }
    }

    private void startResendCooldown(long durationMillis) {
        resendCooldownEndTime = System.currentTimeMillis() + durationMillis;
        resendEmailButton.setEnabled(false);

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendEmailButton.setText(String.format(Locale.getDefault(), "Resend in %ds", millisUntilFinished / 1000));
            }
            @Override
            public void onFinish() {
                resendEmailButton.setText("Resend Verification");
                resendEmailButton.setEnabled(true);
                resendCooldownEndTime = 0;
            }
        }.start();
    }

    // Helper method to dynamically adjust the login button's width
    private void setLoginButtonWeight(float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) loginButton.getLayoutParams();
        params.weight = weight;
        loginButton.setLayoutParams(params);
    }

    // Helper method to control the state of both buttons (Login and Guest)
    private void setButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        loginButton.setClickable(enabled);
        guestButton.setEnabled(enabled);
        guestButton.setClickable(enabled);
    }

    // For configuring the notification
    private void getAndStoreFcmToken(String userId) {
        if (GmsStatus.isGmsAvailable) {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                // Get new FCM registration token
                String token = task.getResult();

                // Create a Map to store the token
                Map<String, Object> tokenData = new HashMap<>();
                tokenData.put("fcmToken", token);

                // Use .set() with SetOptions.merge() for a safe update/create
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(userId)
                        .set(tokenData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d("FCM", "FCM token saved successfully."))
                        .addOnFailureListener(e -> Log.w("FCM", "Error saving FCM token", e));
            });
        } else {
            // This log is helpful for debugging on non-GMS devices
            Log.w("FCM", "GMS not available, skipping FCM token retrieval and storage.");
        }
    }
}
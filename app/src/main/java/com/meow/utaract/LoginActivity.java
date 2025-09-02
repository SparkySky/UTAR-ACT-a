// LoginActivity.java
package com.meow.utaract;

import com.meow.utaract.firebase.AuthService;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseUser;
import com.meow.utaract.ui.gallery.GalleryViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, guestButton;
    private TextView signupTextLink, verificationStatusText;

    // Use a unique request code for starting SignupActivity
    private static final int SIGNUP_REQUEST_CODE = 101;
    public static final String EXTRA_EMAIL_FOR_VERIFICATION_CHECK = "email_for_verification_check";
    public static final String ACTION_EMAIL_VERIFIED = "com.meow.utaract.EMAIL_VERIFIED";

    private ImageButton themeToggleButton; //
    // For SharedPreferences
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "SelectedTheme"; // Can be "light", "dark", or "system"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInputLogin);
        passwordInput = findViewById(R.id.passwordInputLogin);
        loginButton = findViewById(R.id.loginButton);
        guestButton = findViewById(R.id.guestButton);
        signupTextLink = findViewById(R.id.signupTextLink);
        verificationStatusText = findViewById(R.id.verificationStatusText);
        handleIntent(getIntent());

        themeToggleButton = findViewById(R.id.themeToggleButton);

        updateToggleIcon(); // Set initial icon

        themeToggleButton.setOnClickListener(v -> {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                // Currently Dark, switch to Light
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                saveThemePreference("light");
            } else {
                // Currently Light (or unspecified), switch to Dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                saveThemePreference("dark");
            }
            // After changing the mode, the activity might need to be recreated
            // for the icon change to be picked up by updateToggleIcon() in some cases,
            // or for all theme attributes to apply correctly.
            // If you don't recreate, updateToggleIcon() might show the old state's icon
            // until the configuration actually changes. Recreating ensures it.
            // However, try without recreate() first. If the icon updates correctly due to
            // the configuration change broadcast, then it's not strictly needed.
            // If you find the icon doesn't switch immediately, uncomment recreate().
            // recreate();
            // If not recreating, you might need to manually call updateToggleIcon()
            // but it's better if the system handles it via configuration change.
        });

        loginButton.setOnClickListener(v -> loginUser());
        guestButton.setOnClickListener(v -> continueAsGuest());
        signupTextLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivityForResult(intent, SIGNUP_REQUEST_CODE); // Start for result
        });

        // Handle the intent that started this activity (e.g., from email verification link)
        handleIntent(getIntent());
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // The theme has changed, update the icon
        updateToggleIcon();
    }


    private void updateToggleIcon() {
        if (themeToggleButton == null) return; // Guard against null if called too early

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeToggleButton.setImageResource(R.drawable.ic_sun); // In Dark mode, show Sun to switch to Light
            themeToggleButton.setContentDescription("Switch to Light Mode");
        } else {
            themeToggleButton.setImageResource(R.drawable.ic_moon); // In Light mode, show Moon to switch to Dark
            themeToggleButton.setContentDescription("Switch to Dark Mode");
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
            default: // "system" or any other unexpected value
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void saveThemePreference(String themeValue) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_THEME, themeValue);
        editor.apply();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle intent if LoginActivity is already running and is launched again
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && ACTION_EMAIL_VERIFIED.equals(intent.getAction())) {
            String email = intent.getStringExtra(EXTRA_EMAIL_FOR_VERIFICATION_CHECK);
            if (email != null) {
                emailInput.setText(email); // Pre-fill email
                passwordInput.requestFocus(); // Optional: move focus to password
            }
            verificationStatusText.setText("Email verified successfully! Please log in.");
            verificationStatusText.setVisibility(View.VISIBLE);
            // Clear the action so it doesn't trigger again on config change
            getIntent().setAction(null);
        } else if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            // ... your existing deep link handling for clicking the link in the email ...
            // This part is crucial for the case where the user clicks the link
            // and it brings them back to LoginActivity directly.
            Uri data = intent.getData();
            if (data != null && data.toString().startsWith("https://utaract.page.link/verify")) { // Your dynamic link
                FirebaseUser currentUser = new AuthService().getAuth().getCurrentUser();
                String emailFromLink = data.getQueryParameter("email"); // Get email from query param

                if (currentUser != null && currentUser.getEmail().equals(emailFromLink)) {
                    // Current user matches the email in the link, try to reload
                    currentUser.reload().addOnCompleteListener(reloadTask -> {
                        FirebaseUser refreshedUser = new AuthService().getAuth().getCurrentUser();
                        if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                            if (emailFromLink != null) emailInput.setText(emailFromLink);
                            verificationStatusText.setText("Email address verified! You can now log in.");
                            verificationStatusText.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "Email verified. Please log in.", Toast.LENGTH_LONG).show();
                        } else {
                            if (emailFromLink != null) emailInput.setText(emailFromLink);
                            verificationStatusText.setText("Verification link clicked, but status not updated yet. Try logging in.");
                            verificationStatusText.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (emailFromLink != null) {
                    // No current user, or doesn't match. Just prefill email from link.
                    emailInput.setText(emailFromLink);
                    verificationStatusText.setText("Please enter your password to log in.");
                    verificationStatusText.setVisibility(View.VISIBLE);
                }
                // IMPORTANT: The Firebase Email Link authentication (the one that signs the user in with the link)
                // is a different flow. Here we are just using the link as a signal that the email *should* be verified.
                // The user still needs to enter password.
            }
        }
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return;
        }

        boolean isOrganiser = email.endsWith("@utar.my") || email.endsWith("@1utar.my");

        if (isOrganiser == false && (email.endsWith("@utar.my") || email.endsWith("@1utar.my"))) {
            emailInput.setError("Organiser must use @utar.my or @1utar.my email");
            emailInput.requestFocus();
            return;
        }

        verificationStatusText.setVisibility(View.GONE); // Hide verification message on new login attempt

        new AuthService().getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = new AuthService().getAuth().getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();

                            // navigate to MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("IS_ORGANISER", true); // Pass as organizer
                            startActivity(intent);
                            finish();

                        } else if (user != null && !user.isEmailVerified()) {
                            Toast.makeText(LoginActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                            // Offer to resend verification email
                            user.sendEmailVerification();
                            Toast.makeText(LoginActivity.this, "Verify your email address. Check SPAM if you don't see it.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void continueAsGuest() {
        verificationStatusText.setVisibility(View.GONE);
        new AuthService().getAuth().signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Continuing as Guest.", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to your main activity or guest-specific content
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("IS_GUEST_USER", true);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Guest login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGNUP_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String email = data.getStringExtra(EXTRA_EMAIL_FOR_VERIFICATION_CHECK);
                if (email != null) {
                    emailInput.setText(email); // Pre-fill email
                    verificationStatusText.setText("Verification email sent to " + email + ". Please check your inbox and verify, then log in.");
                    verificationStatusText.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = new AuthService().getAuth().getCurrentUser();
        // Go to main activity if guest details existed, else redirect user to fill in.
        if (currentUser != null && currentUser.isEmailVerified()) {
            // If user is already logged in and verified, go to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        else if (currentUser != null && !currentUser.isEmailVerified()) {
            Intent intent = new Intent(LoginActivity.this, GuestFormActivity.class);
            startActivity(intent);
            finish();
        }
    }
}

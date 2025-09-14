package com.meow.utaract.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.meow.utaract.R;

/**
 * SignupActivity handles user registration.
 * - Validates email and password input.
 * - Registers a new user in Firebase Authentication.
 * - Sends verification email after successful signup.
 * - Enforces organiser email domain restrictions.
 */
public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    // UI elements
    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button signupButton;
    private ProgressBar progressBar;

    // Firebase Authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupListeners();
    }

    /**
     * Initialize input fields, button, and progress bar.
     */
    private void initializeViews() {
        emailInput = findViewById(R.id.emailInputSignup);
        passwordInput = findViewById(R.id.passwordInputSignup);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInputSignup);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.signupProgressBar);
    }

    /**
     * Attach listeners to buttons:
     * - Signup button triggers registration.
     * - Login link closes this activity.
     */
    private void setupListeners() {
        signupButton.setOnClickListener(v -> attemptRegistration());
        findViewById(R.id.loginLinkFromSignup).setOnClickListener(v -> finish());
    }

    /**
     * Validates input and attempts to register a new user in Firebase.
     * Enforces organiser email domain rules.
     */
    private void attemptRegistration() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate inputs
        if (!isFormValid(email, password, confirmPassword)) {
            return;
        }

        // Domain validation logic for organisers
        if (isOrganiserEmail(email) == false && (email.endsWith("@utar.my") || email.endsWith("@1utar.my"))) {
            // No-op: user is student with valid domain
        }
        if (isOrganiserEmail(email) && !(email.endsWith("@utar.my") || email.endsWith("@1utar.my"))) {
            emailInput.setError("Organiser must use @utar.my or @1utar.my email");
            emailInput.requestFocus();
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);
        setLoading(true);

        // Register user in Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send verification email
                            sendVerificationEmail(user, email);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignupActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        setLoading(false);
                    }
                });
    }

    /**
     * Checks if the given email belongs to an organiser domain.
     */
    private boolean isOrganiserEmail(String email) {
        return email.endsWith("@utar.my") || email.endsWith("@1utar.my");
    }

    /**
     * Sends a Firebase email verification link.
     * If successful, logs the user out and returns the email for verification check.
     */
    private void sendVerificationEmail(FirebaseUser firebaseUser, String email) {
        // Redirect URL (set in Firebase console)
        String url = "https://utar-act.firebaseapp.com/?email=" + firebaseUser.getEmail();

        // Configure email verification action settings
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl(url)
                .setHandleCodeInApp(true)
                .setAndroidPackageName(getPackageName(), true, null)
                .build();

        // Send verification email
        firebaseUser.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this,
                                "Verification email sent.", Toast.LENGTH_SHORT).show();

                        // Force user to log in again after verification
                        FirebaseAuth.getInstance().signOut();

                        // Return email to LoginActivity for verification flow
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(LoginActivity.EXTRA_EMAIL_FOR_VERIFICATION_CHECK, email);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this,
                                "Failed to send verification email.", Toast.LENGTH_LONG).show();
                        setLoading(false); // Re-enable UI
                    }
                    signupButton.setEnabled(true);
                });
    }

    /**
     * Validates form fields:
     * - Email format
     * - Password length
     * - Confirm password match
     */
    private boolean isFormValid(String email, String password, String confirmPassword) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password should be at least 6 characters");
            passwordInput.requestFocus();
            return false;
        }
        if (!confirmPassword.equals(password)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Shows or hides loading UI (progress bar + disables button).
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            signupButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
        }
    }
}

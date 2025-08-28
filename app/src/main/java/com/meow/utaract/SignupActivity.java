package com.meow.utaract; // Your package name

import com.meow.utaract.firebase.AuthService;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout; // For managing visibility of UI groups

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button signupButton, proceedToLoginButton; // Added proceedToLoginButton
    private TextView loginLink, verificationMessageText; // Added verificationMessageText
    private ProgressBar progressBar;
    private LinearLayout signupFormLayout, verificationStatusLayout; // Layout containers

    private String userEmailForLogin; // To store email to pass to LoginActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup); // Make sure this layout has the new elements

        emailInput = findViewById(R.id.emailInputSignup);
        passwordInput = findViewById(R.id.passwordInputSignup);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInputSignup);
        signupButton = findViewById(R.id.signupButton);
        loginLink = findViewById(R.id.loginLinkFromSignup);
        progressBar = findViewById(R.id.signupProgressBar);

        // New UI Elements for verification status
        signupFormLayout = findViewById(R.id.signupFormLayout); // A LinearLayout holding the form
        verificationStatusLayout = findViewById(R.id.verificationStatusLayout); // A LinearLayout for status
        verificationMessageText = findViewById(R.id.verificationMessageText);
        proceedToLoginButton = findViewById(R.id.proceedToLoginButton);


        signupButton.setOnClickListener(v -> registerUser());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        proceedToLoginButton.setOnClickListener(v -> checkVerificationStatusAndProceed());

        // Initially, show signup form and hide verification status
        showSignupForm();
    }

    private void showSignupForm() {
        signupFormLayout.setVisibility(View.VISIBLE);
        verificationStatusLayout.setVisibility(View.GONE);
        loginLink.setVisibility(View.VISIBLE); // Show "Already have an account? Login"
    }

    private void showVerificationStatus(String email) {
        userEmailForLogin = email; // Store for later
        signupFormLayout.setVisibility(View.GONE);
        verificationStatusLayout.setVisibility(View.VISIBLE);
        loginLink.setVisibility(View.GONE); // Hide "Already have an account? Login"
        verificationMessageText.setText("Verification email sent to " + email + ".\nPlease check your inbox and click the link to verify your account.");
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

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
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password should be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }
        if (!confirmPassword.equals(password)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);

        new AuthService().getAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    // Keep signupButton disabled until flow is complete or reset

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = new AuthService().getAuth().getCurrentUser();
                        if (firebaseUser != null) {
                            sendVerificationEmail(firebaseUser, email);
                        } else {
                            Toast.makeText(SignupActivity.this, "Registration successful, but failed to get user.", Toast.LENGTH_SHORT).show();
                            signupButton.setEnabled(true); // Re-enable if something went wrong before email send
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("SignupActivity", "createUser", task.getException());
                        signupButton.setEnabled(true); // Re-enable
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser, String email) {
        // Prepare ActionCodeSettings for email verification deep link
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://utaract.link/verify?email=" + firebaseUser.getEmail()) // Make sure this is your dynamic link
                .setHandleCodeInApp(true) // Important for deep linking
                .setAndroidPackageName(
                        getPackageName(),
                        true, /* installIfNotAvailable */
                        "21"  /* minimumVersion, can be your app's minSdkVersion */)
                .build();

        firebaseUser.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener(verificationTask -> {
                    if (verificationTask.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                        showVerificationStatus(email); // Switch UI
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to send verification email: " + verificationTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("SignupActivity", "sendEmailVerification", verificationTask.getException());
                        signupButton.setEnabled(true); // Re-enable signup button if email send failed
                    }
                });
    }

    private void checkVerificationStatusAndProceed() {
        FirebaseUser currentUser = new AuthService().getAuth().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user session found. Please try signing up again.", Toast.LENGTH_LONG).show();
            showSignupForm(); // Reset to signup form
            signupButton.setEnabled(true);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        proceedToLoginButton.setEnabled(false);

        currentUser.reload().addOnCompleteListener(reloadTask -> {
            progressBar.setVisibility(View.GONE);
            proceedToLoginButton.setEnabled(true);

            if (reloadTask.isSuccessful()) {
                FirebaseUser refreshedUser = new AuthService().getAuth().getCurrentUser(); // Get the latest user state
                if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                    Toast.makeText(SignupActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();

                    // Pass data back to LoginActivity
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    // Pass email so LoginActivity can pre-fill it and show a message
                    intent.putExtra(LoginActivity.EXTRA_EMAIL_FOR_VERIFICATION_CHECK, userEmailForLogin);
                    intent.setAction(LoginActivity.ACTION_EMAIL_VERIFIED); // Custom action
                    // Set flags to clear stack and make LoginActivity the new root
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish(); // Finish SignupActivity
                } else {
                    Toast.makeText(SignupActivity.this, "Email not yet verified. Please check your email or resend.", Toast.LENGTH_LONG).show();
                    // Optionally: Add a "Resend Verification Email" button here
                }
            } else {
                Toast.makeText(SignupActivity.this, "Failed to check verification status: " + reloadTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("SignupActivity", "reloadUser", reloadTask.getException());
            }
        });
    }
}

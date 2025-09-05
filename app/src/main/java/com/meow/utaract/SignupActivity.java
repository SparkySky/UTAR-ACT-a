package com.meow.utaract;

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

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button signupButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInputSignup);
        passwordInput = findViewById(R.id.passwordInputSignup);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInputSignup);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.signupProgressBar);
    }

    private void setupListeners() {
        signupButton.setOnClickListener(v -> attemptRegistration());
        findViewById(R.id.loginLinkFromSignup).setOnClickListener(v -> finish());
    }

    private void attemptRegistration() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!isFormValid(email, password, confirmPassword)) {
            return;
        }


        if (isOrganiserEmail(email) == false && (email.endsWith("@utar.my") || email.endsWith("@1utar.my"))) {
        }
        if (isOrganiserEmail(email) && !(email.endsWith("@utar.my") || email.endsWith("@1utar.my"))) {
            emailInput.setError("Organiser must use @utar.my or @1utar.my email");
            emailInput.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);
//
        setLoading(true);


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendVerificationEmail(user, email);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignupActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                    }
                });
    }


    private boolean isOrganiserEmail(String email) {
        return email.endsWith("@utar.my") || email.endsWith("@1utar.my");
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser, String email) {
        // Actual auth domain from the Firebase console
        String url = "https://utar-act.firebaseapp.com/?email=" + firebaseUser.getEmail();

        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl(url)
                .setHandleCodeInApp(true)
                .setAndroidPackageName(getPackageName(), true, null)
                .build();

        firebaseUser.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();

                        // Sign the user out so they have to log in and be verified
                        FirebaseAuth.getInstance().signOut();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(LoginActivity.EXTRA_EMAIL_FOR_VERIFICATION_CHECK, email);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to send verification email.", Toast.LENGTH_LONG).show();

                        setLoading(false); // Make sure to re-enable button on failure
                    }
                    signupButton.setEnabled(true);
                });
    }


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
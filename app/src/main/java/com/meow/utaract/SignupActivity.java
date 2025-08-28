package com.meow.utaract;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseUser;
import com.meow.utaract.firebase.AuthService;

public class SignupActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button signupButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

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
        signupButton.setOnClickListener(v -> registerUser());
        findViewById(R.id.loginLinkFromSignup).setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!isFormValid(email, password, confirmPassword)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);

        new AuthService().getAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = new AuthService().getAuth().getCurrentUser();
                        if (firebaseUser != null) {
                            sendVerificationEmail(firebaseUser, email);
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        signupButton.setEnabled(true);
                    }
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

    private void sendVerificationEmail(FirebaseUser firebaseUser, String email) {
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://utaract.page.link/verify?email=" + firebaseUser.getEmail())
                .setHandleCodeInApp(true)
                .setAndroidPackageName(getPackageName(), true, "24")
                .build();

        firebaseUser.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener(verificationTask -> {
                    if (verificationTask.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(LoginActivity.EXTRA_EMAIL_FOR_VERIFICATION_CHECK, email);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to send verification email.", Toast.LENGTH_LONG).show();
                        signupButton.setEnabled(true);
                    }
                });
    }
}
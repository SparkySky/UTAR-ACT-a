package com.meow.utaract.firebase;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class AuthService {
    private final FirebaseAuth firebaseAuth;

    public AuthService() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public FirebaseAuth getAuth() {
        return firebaseAuth;
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public void signInAnonymously(Activity activity) {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(activity, "Signed in anonymously", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "Anonymous sign-in failed", Toast.LENGTH_SHORT).show();
                            Log.e("AuthService", "signInAnonymously failed", task.getException());
                        }
                    }
                });
    }
}

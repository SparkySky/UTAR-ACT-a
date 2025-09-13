package com.meow.utaract;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        checkIfGmsPackageIsPresent();

        // Force light mode unless user preference overrides it
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        // Init light/dark mode from preference
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean("isNightMode", false); // Default to light mode
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        FirebaseApp.initializeApp(this);
        // Initialize Firebase App Check
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();

        if (GmsStatus.isGmsAvailable) {
            // GMS is available, initialize GMS-dependent services
            Log.d(TAG, "GMS is available. Initializing App Check.");
            // Check if the app is in a debuggable state
            boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                Log.d(TAG, "Debug build detected. Using DebugAppCheckProviderFactory.");
                // Use the debug provider for debug builds
                firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance());
            } else {
                Log.d(TAG, "Release build detected. Using PlayIntegrityAppCheckProviderFactory.");
                // Use Play Integrity for release builds
                firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance());
            }
        } else {
            // GMS is NOT available. Do not initialize App Check.
            Log.w(TAG, "GMS not available. Skipping App Check initialization.");
        }
    }

    private void checkIfGmsPackageIsPresent() {
        try {
            getPackageManager().getPackageInfo("com.google.android.gms", 0);
            GmsStatus.isGmsAvailable = true;
            Log.d("GMS_CHECK", "GMS package is present on the device.");
        } catch (PackageManager.NameNotFoundException e) {
            GmsStatus.isGmsAvailable = false;
            Log.w("GMS_CHECK", "GMS package is NOT present on the device.");
        }
    }
}

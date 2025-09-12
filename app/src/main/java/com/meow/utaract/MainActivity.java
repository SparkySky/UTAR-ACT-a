package com.meow.utaract;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.navigation.NavigationView;
import com.meow.utaract.databinding.ActivityMainBinding;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel mainViewModel;
    private boolean isOrganiser;
    public boolean isOrganiser() { return isOrganiser; }

    // Declare the ActivityResultLauncher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. You can show a message or do nothing.
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied.
                    Toast.makeText(this, "Notifications permission denied. You may miss important updates.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);
        mainViewModel.setOrganiser(isOrganiser);

        setupNavigation(isOrganiser);

        // Ask for notification permission when the main activity is created.
        askNotificationPermission();

        //// Below are for the light and dark mode toggle
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);
        ToggleButton themeToggleButton = headerView.findViewById(R.id.themeToggleButton);

        // Set initial state of the toggle button
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean("isNightMode", false);
        if (isNightMode) {
            themeToggleButton.setChecked(true);
            themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sun, 0, 0, 0);
        } else {
            themeToggleButton.setChecked(false);
            themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_moon, 0, 0, 0);
        }


        themeToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("isNightMode", true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("isNightMode", false);
            }
            editor.apply();
        });
    }

    private void askNotificationPermission() {
        // This is only necessary for API level 33 and above (Android 13)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupNavigation(boolean isOrganiser) {
        NavigationView navigationView = binding.navView;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();

        // This line is enough to make the navigation drawer items work.
        NavigationUI.setupWithNavController(navigationView, navController);

        // Set custom navigation item selected listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            DrawerLayout drawer = findViewById(R.id.drawer_layout);

            if (id == R.id.nav_manage_events) {
                // Launch ManageEventsActivity instead of using navigation component
                Intent intent = new Intent(MainActivity.this, ManageEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                // Close the drawer
                binding.drawerLayout.closeDrawer(binding.navView);
            } else if (id == R.id.nav_news) {
                // Launch NewsActivity
                Intent intent = new Intent(MainActivity.this, NewsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                binding.drawerLayout.closeDrawer(binding.navView);
            } else if (id == R.id.nav_joined_events) {
                Intent intent = new Intent(MainActivity.this, JoinedEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        Menu navMenu = navigationView.getMenu();
        navMenu.findItem(R.id.nav_manage_events).setVisible(isOrganiser);
    }

    private void loadUserProfile(boolean isOrganiser) {
        GuestProfileStorage storage = new GuestProfileStorage(this);
        if (isOrganiser && !storage.profileExists()) {
            storage.downloadProfileFromFirestore(new GuestProfileStorage.FirestoreCallback() {
                @Override
                public void onSuccess(GuestProfile user) { storage.saveProfile(user); }
                @Override
                public void onFailure(Exception e) {
                    Intent intent = new Intent(MainActivity.this, GuestFormActivity.class);
                    intent.putExtra("IS_ORGANISER", true);
                    startActivity(intent);
                    finish();
                }
            });
        } else if (!isOrganiser && !storage.profileExists()) {
            Intent intent = new Intent(MainActivity.this, GuestFormActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public DrawerLayout getDrawerLayout() {
        return binding.drawerLayout;
    }
}
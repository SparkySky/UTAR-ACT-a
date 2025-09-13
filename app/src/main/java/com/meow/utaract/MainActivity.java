package com.meow.utaract;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.databinding.ActivityMainBinding;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel mainViewModel;
    private boolean isOrganiser;
    private TextView tvNavUsername;
    private TextView tvNavEmail;
    private CircleImageView navProfileImage;

    public boolean isOrganiser() { return isOrganiser; }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications permission denied. You may miss important updates.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);
        GuestProfileStorage profileStorage = new GuestProfileStorage(this);

        if (isOrganiser) {
            // For organizers, first attempt to download the profile from Firestore
            profileStorage.downloadProfileFromFirestore(new GuestProfileStorage.FirestoreCallback() {
                @Override
                public void onSuccess(GuestProfile profile) {
                    // Profile downloaded, now proceed to check and start the app
                    checkAndStartApp(profile);
                }

                @Override
                public void onFailure(Exception e) {
                    // Download failed, assume no profile exists and redirect to the form
                    redirectToGuestForm();
                }
            });
        } else {
            // For guests, check the local profile directly
            GuestProfile profile = profileStorage.loadProfile();
            checkAndStartApp(profile);
        }
    }

    private void checkAndStartApp(GuestProfile profile) {
        if (profile == null || (profile.getName() == null || profile.getName().isEmpty() ||
                profile.getEmail() == null || profile.getEmail().isEmpty() ||
                profile.getPhone() == null || profile.getPhone().isEmpty())) {
            // Profile is incomplete or does not exist
            redirectToGuestForm();
        } else {
            // Profile is complete, start the main UI
            startApp();
        }
    }

    private void startApp() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupNavigation(isOrganiser);
        setupThemeToggle();
        askNotificationPermission();
        mainViewModel.setOrganiser(isOrganiser);
    }

    private void redirectToGuestForm() {
        Toast.makeText(this, "Please complete your profile to continue.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, GuestFormActivity.class);
        intent.putExtra("IS_ORGANISER", isOrganiser);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainViewModel != null) {
            mainViewModel.setOrganiser(isOrganiser);
        }
    }

    private void setupThemeToggle() {
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);
        ToggleButton themeToggleButton = headerView.findViewById(R.id.themeToggleButton);
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean("isNightMode", false);
        themeToggleButton.setChecked(isNightMode);
        themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(isNightMode ? R.drawable.ic_sun : R.drawable.ic_moon, 0, 0, 0);

        themeToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isNightMode", isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            themeToggleButton.setCompoundDrawablesWithIntrinsicBounds(isChecked ? R.drawable.ic_sun : R.drawable.ic_moon, 0, 0, 0);
        });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupNavigation(boolean isOrganiser) {
        NavigationView navigationView = binding.navView;
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(navigationView, navController);

        View headerView = navigationView.getHeaderView(0);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            DrawerLayout drawer = binding.drawerLayout;
            if (id == R.id.nav_manage_events) {
                Intent intent = new Intent(MainActivity.this, ManageEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
            } else if (id == R.id.nav_news) {
                Intent intent = new Intent(MainActivity.this, NewsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
            } else if (id == R.id.nav_joined_events) {
                Intent intent = new Intent(MainActivity.this, JoinedEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
            } else {
                NavigationUI.onNavDestinationSelected(item, navController);
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        Menu navMenu = navigationView.getMenu();
        navMenu.findItem(R.id.nav_manage_events).setVisible(isOrganiser);
    }

    public DrawerLayout getDrawerLayout() {
        return binding.drawerLayout;
    }
}
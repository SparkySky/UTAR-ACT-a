package com.meow.utaract;

import com.meow.utaract.utils.GuestProfileStorage;
import com.meow.utaract.utils.GuestProfile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GuestProfile user;

        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Used for profile storage
        GuestProfileStorage storage = new GuestProfileStorage(this);
        boolean isOrganizer = getIntent().getBooleanExtra("IS_ORGANISER", false);

        // Try downloading from Firestore first - Only for logged in users
        if (isOrganizer) {
            storage.downloadProfileFromFirestore(new GuestProfileStorage.FirestoreCallback() {
                @Override
                public void onSuccess(GuestProfile user) {
                    // Save JSON locally
                    storage.saveProfile(user);
                    // Initialize the user data
                    setupMainUI(user);
                }
                // User didn't fill up previously, redirect to form
                @Override
                public void onFailure(Exception e) {
                    // If no profile found, redirect to form
                    Intent intent = new Intent(MainActivity.this, GuestFormActivity.class);
                    intent.putExtra("IS_ORGANISER", true);
                    startActivity(intent);
                    finish();
                }
            });
        }
        else if (!storage.profileExists()) {
            // If no profile found, redirect to form
            Intent intent = new Intent(MainActivity.this, GuestFormActivity.class);
            intent.putExtra("IS_ORGANISER", false);
            startActivity(intent);
            finish();
        }
        else {
            // Load Guest/Event Organiser data from local profile JSON file
            // E.g. user.getName(),...
            user = storage.loadProfile();
            // Initialize the user data
            setupMainUI(user);
        }

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile_setting) { // Edit profile
            Intent intent = new Intent(MainActivity.this, GuestFormActivity.class);
            intent.putExtra("IS_EDIT", true);
            intent.putExtra("IS_ORGANISER", true);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_logout) {
            // Clear saved JSON profile
            GuestProfileStorage storage = new GuestProfileStorage(this);
            storage.clearProfile(); // Delete the JSON profile file entry

            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // Redirect to login screen
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // User Data Storing
    public void setupMainUI(GuestProfile profile) {
      String name = profile.getName();
      String email = profile.getEmail();
      String phone = profile.getPhone();
      String preferences = String.join(", ", profile.getPreferences());
    }
}
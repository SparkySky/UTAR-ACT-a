package com.meow.utaract;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
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

    public boolean isOrganiser() {
        return isOrganiser;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);
        mainViewModel.setOrganiser(isOrganiser);

        setupNavigation(isOrganiser);
        loadUserProfile(isOrganiser);
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

            if (id == R.id.nav_manage_events) {
                // Launch ManageEventsActivity instead of using navigation component
                Intent intent = new Intent(MainActivity.this, ManageEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);

                // Close the drawer
                binding.drawerLayout.closeDrawer(binding.navView);
                return true;
            } else if (id == R.id.nav_news) {
                // Launch NewsActivity
                Intent intent = new Intent(MainActivity.this, NewsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                binding.drawerLayout.closeDrawer(binding.navView);
                return true;
            } else {
                // For all other items, use the default navigation
                try {
                    NavigationUI.onNavDestinationSelected(item, navController);
                    binding.drawerLayout.closeDrawer(binding.navView);
                    return true;
                } catch (IllegalArgumentException e) {
                    // Handle case where the destination doesn't exist in the navigation graph
                    return false;
                }
            }
        });

        Menu navMenu = navigationView.getMenu();
        navMenu.findItem(R.id.nav_manage_events).setVisible(isOrganiser);
    }

    // The onSupportNavigateUp method is no longer needed.

    private void openNewsActivity() {
        Intent intent = new Intent(this, NewsActivity.class);
        intent.putExtra("IS_ORGANISER", isOrganiser); // PASS THE FLAG
        startActivity(intent);
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
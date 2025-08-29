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
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        boolean isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);
        mainViewModel.setOrganiser(isOrganiser);

        setupNavigation(isOrganiser);
        loadUserProfile(isOrganiser);
    }

    private void setupNavigation(boolean isOrganiser) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationView navView = binding.navView;

            // Show/Hide "My Events" menu item based on user role
            Menu navMenu = navView.getMenu();
            navMenu.findItem(R.id.nav_my_events).setVisible(isOrganiser);

            // Handle navigation clicks
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_my_events) {
                    startActivity(new Intent(this, MyEventsActivity.class));
                    binding.drawerLayout.closeDrawers();
                    return true;
                }
                // Let NavigationUI handle other clicks
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (handled) {
                    binding.drawerLayout.closeDrawers();
                }
                return handled;
            });
        }
    }

    private void loadUserProfile(boolean isOrganiser) {
        GuestProfileStorage storage = new GuestProfileStorage(this);
        if (isOrganiser && !storage.profileExists()) {
            storage.downloadProfileFromFirestore(new GuestProfileStorage.FirestoreCallback() {
                @Override
                public void onSuccess(GuestProfile user) {
                    storage.saveProfile(user);
                }
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

    // This method is now public so the fragment can access the DrawerLayout
    public DrawerLayout getDrawerLayout() {
        return binding.drawerLayout;
    }
}
package com.meow.utaract.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.R;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import com.meow.utaract.utils.News;
import com.meow.utaract.adapters.NewsAdapter;
import com.meow.utaract.utils.NewsStorage;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying and managing News.
 *
 * Features:
 * - Organisers can create, edit, and delete news.
 * - Guests can only view news from organisers they follow.
 * - Includes navigation drawer for app navigation.
 */
public class NewsActivity extends AppCompatActivity implements NewsAdapter.NewsItemClickListener {

    private RecyclerView newsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private NewsAdapter newsAdapter;
    private NewsStorage newsStorage;
    private GuestProfileStorage profileStorage;
    private boolean isOrganiser;
    private DrawerLayout drawerLayout;
    private ImageView menuIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup drawer and navigation view
        drawerLayout = findViewById(R.id.drawer_layout);
        menuIcon = findViewById(R.id.menu_icon);
        NavigationView navView = findViewById(R.id.nav_view);

        // Enable group dividers in the navigation menu
        MenuCompat.setGroupDividerEnabled(navView.getMenu(), true);

        // Open drawer when menu icon clicked
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Determine if user is organiser
        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);

        // Initialize views and storage utilities
        initializeViews();
        newsStorage = new NewsStorage();
        profileStorage = new GuestProfileStorage(this);

        // Show/hide organiser-only menu items
        navView.getMenu().findItem(R.id.nav_manage_events).setVisible(isOrganiser);

        // Floating Action Button for creating news (organisers only)
        FloatingActionButton fabCreateNews = findViewById(R.id.fabCreateNews);
        fabCreateNews.setOnClickListener(v -> {
            Intent intent = new Intent(NewsActivity.this, NewsCreationActivity.class);
            startActivity(intent);
        });
        fabCreateNews.setVisibility(isOrganiser ? View.VISIBLE : View.GONE);

        // Handle navigation drawer clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Go to Home
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_manage_events) {
                // Go to Manage Events
                Intent intent = new Intent(this, ManageEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_joined_events) {
                // Go to Joined Events
                Intent intent = new Intent(this, JoinedEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Load view based on user type
        if (isOrganiser) {
            setupOrganiserView();
        } else {
            setupGuestView();
        }
    }

    /**
     * Initializes RecyclerView, ProgressBar, Empty View, and Adapter.
     */
    private void initializeViews() {
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        progressBar = findViewById(R.id.newsProgressBar);
        emptyView = findViewById(R.id.newsEmptyView);

        // Pass "true" to enable click listeners, and isOrganiser to control edit/delete visibility
        newsAdapter = new NewsAdapter(new ArrayList<>(), this, true, isOrganiser);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsRecyclerView.setAdapter(newsAdapter);
    }

    /**
     * Organiser view setup: show FAB and load organiser news.
     */
    private void setupOrganiserView() {
        findViewById(R.id.fabCreateNews).setVisibility(View.VISIBLE);
        findViewById(R.id.fabCreateNews).setOnClickListener(v -> {
            startActivity(new Intent(this, NewsCreationActivity.class));
        });
        loadOrganiserNewsWithFollowing();
    }

    /**
     * Guest view setup: hide FAB and load news from followed organisers.
     */
    private void setupGuestView() {
        findViewById(R.id.fabCreateNews).setVisibility(View.GONE);
        loadGuestNews();
    }

    /**
     * Loads organiser news along with news from followed organisers.
     */
    private void loadOrganiserNewsWithFollowing() {
        progressBar.setVisibility(View.VISIBLE);
        String currentOrganizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load organiser profile to retrieve followed organisers
        GuestProfile profile = profileStorage.loadProfile();
        List<String> followedOrganizerIds = (profile != null && profile.getFollowing() != null) ?
                profile.getFollowing() : new ArrayList<>();

        // Fetch news for organiser + followed organisers
        newsStorage.getNewsForOrganizerWithFollowing(currentOrganizerId, followedOrganizerIds,
                new NewsStorage.NewsListCallback() {
                    @Override
                    public void onSuccess(List<News> newsList) {
                        progressBar.setVisibility(View.GONE);
                        updateUI(newsList);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        Log.e("NewsDebug", "Failed to load organizer news with following: " + e.getMessage());
                    }
                });
    }

    /**
     * Loads news for guest users (only from organisers they follow).
     */
    private void loadGuestNews() {
        progressBar.setVisibility(View.VISIBLE);

        GuestProfile profile = new GuestProfileStorage(this).loadProfile();
        if (profile == null || profile.getFollowing() == null || profile.getFollowing().isEmpty()) {
            // No organisers followed
            progressBar.setVisibility(View.GONE);
            emptyView.setText("You're not following any organizers yet.");
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        // Fetch news for followed organisers
        newsStorage.getNewsForGuest(profile.getFollowing(), new NewsStorage.NewsListCallback() {
            @Override
            public void onSuccess(List<News> newsList) {
                progressBar.setVisibility(View.GONE);
                updateUI(newsList);
            }
            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Updates UI by showing/hiding views based on news availability.
     */
    private void updateUI(List<News> newsList) {
        if (newsList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            newsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            newsRecyclerView.setVisibility(View.VISIBLE);
            newsAdapter.updateNews(newsList);
        }
    }

    /**
     * Handles edit action for a news item.
     */
    @Override
    public void onEditClicked(News news, int position) {
        Intent intent = new Intent(this, NewsCreationActivity.class);
        intent.putExtra("EDIT_NEWS", news);
        intent.putExtra("IS_EDIT_MODE", true);
        startActivity(intent);
    }

    /**
     * Handles delete action for a news item (with confirmation).
     */
    @Override
    public void onDeleteClicked(News news, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete News")
                .setMessage("Are you sure you want to delete this news? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteNews(news, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes a news item from Firestore and refreshes list.
     */
    private void deleteNews(News news, int position) {
        NewsStorage newsStorage = new NewsStorage();
        newsStorage.deleteNews(news.getNewsId(), new NewsStorage.NewsCallback() {
            @Override
            public void onSuccess(String newsId) {
                runOnUiThread(() -> {
                    Toast.makeText(NewsActivity.this, "News deleted successfully", Toast.LENGTH_SHORT).show();
                    // Refresh list based on role
                    if (isOrganiser) {
                        loadOrganiserNewsWithFollowing();
                    } else {
                        loadGuestNews();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewsActivity.this, "Failed to delete news: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh news whenever activity resumes
        if (isOrganiser) {
            loadOrganiserNewsWithFollowing();
        } else {
            loadGuestNews();
        }
    }
}

package com.meow.utaract;

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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import com.meow.utaract.utils.News;
import com.meow.utaract.utils.NewsAdapter;
import com.meow.utaract.utils.NewsStorage;
import java.util.ArrayList;
import java.util.List;

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        drawerLayout = findViewById(R.id.drawer_layout);
        menuIcon = findViewById(R.id.menu_icon);
        NavigationView navView = findViewById(R.id.nav_view);

        menuIcon.setOnClickListener(v -> {
            // Open the drawer when icon is clicked
            drawerLayout.openDrawer(GravityCompat.START);
        });

        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);

        initializeViews();
        newsStorage = new NewsStorage();
        profileStorage = new GuestProfileStorage(this);

        navView.getMenu().findItem(R.id.nav_manage_events).setVisible(isOrganiser);

        // Set up FAB click listener
        FloatingActionButton fabCreateNews = findViewById(R.id.fabCreateNews);
        fabCreateNews.setOnClickListener(v -> {
            Intent intent = new Intent(NewsActivity.this, NewsCreationActivity.class);
            startActivity(intent);
        });

        // Show/hide FAB based on user type
        fabCreateNews.setVisibility(isOrganiser ? View.VISIBLE : View.GONE);

        // Handle navigation item clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(NewsActivity.this, MainActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_manage_events) {
                Intent intent = new Intent(NewsActivity.this, ManageEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                finish();
            }
            // Close drawer after selection
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        if (isOrganiser) {
            setupOrganiserView();
        } else {
            setupGuestView();
        }
    }

    private void initializeViews() {
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        progressBar = findViewById(R.id.newsProgressBar);
        emptyView = findViewById(R.id.newsEmptyView);

        newsAdapter = new NewsAdapter(new ArrayList<>(), this, true, isOrganiser);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsRecyclerView.setAdapter(newsAdapter);
    }


    private void setupOrganiserView() {
        // Organizers can create news and see their own news
        findViewById(R.id.fabCreateNews).setVisibility(View.VISIBLE);
        findViewById(R.id.fabCreateNews).setOnClickListener(v -> {
            startActivity(new Intent(this, NewsCreationActivity.class));
        });
        loadOrganiserNewsWithFollowing();
    }

    private void setupGuestView() {
        // Guests can only view news from organizers they follow
        findViewById(R.id.fabCreateNews).setVisibility(View.GONE);
        loadGuestNews();
    }

    private void loadOrganiserNewsWithFollowing() {
        progressBar.setVisibility(View.VISIBLE);
        String currentOrganizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load the organizer's profile to get followed organizers
        GuestProfile profile = profileStorage.loadProfile();
        List<String> followedOrganizerIds = (profile != null && profile.getFollowing() != null) ?
                profile.getFollowing() : new ArrayList<>();

        // Get news from current organizer + followed organizers
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

    private void loadGuestNews() {
        progressBar.setVisibility(View.VISIBLE);

        GuestProfile profile = new GuestProfileStorage(this).loadProfile();
        if (profile == null || profile.getFollowing() == null || profile.getFollowing().isEmpty()) {
            progressBar.setVisibility(View.GONE);
            emptyView.setText("You're not following any organizers yet.");
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

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
    @Override
    public void onEditClicked(News news, int position) {
        // Open NewsCreationActivity in edit mode
        Intent intent = new Intent(this, NewsCreationActivity.class);
        intent.putExtra("EDIT_NEWS", news);
        intent.putExtra("IS_EDIT_MODE", true);
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(News news, int position) {
        // Show confirmation dialog before deletion
        new AlertDialog.Builder(this)
                .setTitle("Delete News")
                .setMessage("Are you sure you want to delete this news? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteNews(news, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNews(News news, int position) {
        NewsStorage newsStorage = new NewsStorage();
        newsStorage.deleteNews(news.getNewsId(), new NewsStorage.NewsCallback() {
            @Override
            public void onSuccess(String newsId) {
                runOnUiThread(() -> {
                    Toast.makeText(NewsActivity.this, "News deleted successfully", Toast.LENGTH_SHORT).show();
                    // Refresh the news list
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
        // Refresh news when returning to this activity
        if (isOrganiser) {
            loadOrganiserNewsWithFollowing();
        } else {
            loadGuestNews();
        }
    }
}
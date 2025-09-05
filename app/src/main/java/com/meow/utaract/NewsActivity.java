package com.meow.utaract;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.utils.News;
import com.meow.utaract.utils.NewsAdapter;
import com.meow.utaract.utils.NewsStorage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NewsActivity extends AppCompatActivity implements NewsAdapter.NewsInteractionListener {

    private RecyclerView newsRecyclerView;
    private NewsAdapter newsAdapter;
    private TextView emptyView;
    private ProgressBar progressBar;
    private EditText searchInput;
    private DrawerLayout drawerLayout;
    private boolean isOrganizer;

    private NewsStorage newsStorage;
    private List<News> allNews = new ArrayList<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize NewsStorage
        newsStorage = new NewsStorage(this);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Get user type from intent
        isOrganizer = getIntent().getBooleanExtra("IS_ORGANISER", false);

        // Set up navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Set up menu icon to open drawer
        findViewById(R.id.menu_icon).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Set up back button in toolbar
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                finish();
            }
        });

        // Set up navigation item selection
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_news) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, NewsActivity.class);
                intent.putExtra("IS_ORGANISER", true); // or false for guests
                startActivity(intent);
            } else if (id == R.id.nav_manage_events) {
                Intent intent = new Intent(this, ManageEventsActivity.class);
                startActivity(intent);
                finish();
            }

            return true;
        });

        // Initialize views
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        searchInput = findViewById(R.id.search_input);
        FloatingActionButton fabAddNews = findViewById(R.id.fabAddNews);

        // Show FAB only for organizers
        fabAddNews.setVisibility(isOrganizer ? View.VISIBLE : View.GONE);
        fabAddNews.setOnClickListener(v -> showAddNewsDialog());

        setupRecyclerView();
        setupSearchFunctionality();
        loadNews();
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(new ArrayList<>(), this, this);
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsRecyclerView.setAdapter(newsAdapter);
    }

    private void setupSearchFunctionality() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNews(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterNews(String query) {
        if (query.isEmpty()) {
            newsAdapter.updateNews(allNews);
            return;
        }

        List<News> filteredNews = allNews.stream()
                .filter(news -> news.getContent().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        newsAdapter.updateNews(filteredNews);

        if (filteredNews.isEmpty()) {
            emptyView.setText("No news matching your search");
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void loadNews() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        if (isOrganizer) {
            // Organizers see all news
            newsStorage.getAllNews(new NewsStorage.NewsFetchCallback() {
                @Override
                public void onSuccess(List<News> newsList) {
                    allNews = newsList;
                    progressBar.setVisibility(View.GONE);

                    if (newsList.isEmpty()) {
                        emptyView.setText("No news yet. Be the first to post!");
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        newsAdapter.updateNews(newsList);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setText("Failed to load news");
                    emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(NewsActivity.this, "Error loading news: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Guests see only news from followed organizers
            newsStorage.getNewsFromFollowedOrganizers(new NewsStorage.NewsFetchCallback() {
                @Override
                public void onSuccess(List<News> newsList) {
                    allNews = newsList;
                    progressBar.setVisibility(View.GONE);

                    if (newsList.isEmpty()) {
                        emptyView.setText("No news from followed organizers yet");
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        newsAdapter.updateNews(newsList);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setText("Failed to load news");
                    emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(NewsActivity.this, "Error loading news: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showAddNewsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Add News");

        final EditText input = new EditText(this);
        input.setHint("Enter your news content");
        input.setMinLines(3);
        input.setMaxLines(5);

        builder.setView(input);

        builder.setPositiveButton("Post", (dialog, which) -> {
            String content = input.getText().toString().trim();
            if (!content.isEmpty()) {
                postNews(content);
            } else {
                Toast.makeText(this, "Please enter news content", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void postNews(String content) {
        progressBar.setVisibility(View.VISIBLE);

        newsStorage.createNews(content, new NewsStorage.NewsCreationCallback() {
            @Override
            public void onSuccess(String newsId) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewsActivity.this, "News posted successfully!", Toast.LENGTH_SHORT).show();

                // Add the new news to local list immediately for better UX
                News newNews = new News();
                newNews.setNewsId(newsId);
                newNews.setOrganizerId(currentUserId);
                newNews.setContent(content);
                newNews.setPostedDate(new Date());
                newNews.setLikeCount(0);
                newNews.setLikedBy(new ArrayList<>());

                // Add to beginning of list
                allNews.add(0, newNews);
                newsAdapter.updateNews(allNews);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                if (e.getMessage().contains("PERMISSION_DENIED")) {
                    Toast.makeText(NewsActivity.this, "Permission denied. Please check your organizer status.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NewsActivity.this, "Failed to post news: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onLikeNews(News news) {
        newsStorage.likeNews(news.getNewsId(), currentUserId, new NewsStorage.LikeCallback() {
            @Override
            public void onSuccess() {
                // Update local data
                news.setLikeCount(news.getLikeCount() + 1);
                if (!news.getLikedBy().contains(currentUserId)) {
                    news.getLikedBy().add(currentUserId);
                }
                newsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(NewsActivity.this, "Failed to like news", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUnlikeNews(News news) {
        newsStorage.unlikeNews(news.getNewsId(), currentUserId, new NewsStorage.LikeCallback() {
            @Override
            public void onSuccess() {
                // Update local data
                news.setLikeCount(news.getLikeCount() - 1);
                news.getLikedBy().remove(currentUserId);
                newsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(NewsActivity.this, "Failed to unlike news", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
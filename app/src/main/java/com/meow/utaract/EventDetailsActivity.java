package com.meow.utaract;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EventDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Get event data from intent
        String eventId = getIntent().getStringExtra("event_id");
        String eventTitle = getIntent().getStringExtra("event_title");
        String eventDescription = getIntent().getStringExtra("event_description");
        String eventDate = getIntent().getStringExtra("event_date");
        String eventTime = getIntent().getStringExtra("event_time");
        String eventLocation = getIntent().getStringExtra("event_location");
        String eventAudience = getIntent().getStringExtra("event_audience");
        String eventCategory = getIntent().getStringExtra("event_category");
        String eventOrganizer = getIntent().getStringExtra("event_organizer");
        String eventBannerUrl = getIntent().getStringExtra("event_banner_url");

        // Setup toolbar
        setupToolbar();

        // Populate event details
        populateEventDetails(eventTitle, eventDescription, eventDate, eventTime, 
                           eventLocation, eventAudience, eventCategory, eventOrganizer);

        // Setup action buttons
        setupActionButtons();
    }

    private void setupToolbar() {
        // Back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        // Title
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText("Event details");
    }

    private void populateEventDetails(String title, String description, String date, 
                                    String time, String location, String audience, 
                                    String category, String organizer) {
        
        // Event banner and title overlay
        TextView bannerTitle = findViewById(R.id.event_title);
        bannerTitle.setText(title);

        // Main event title
        TextView eventTitle = findViewById(R.id.event_title);
        eventTitle.setText("An Expert Talk: " + title);

        // Date and time
        TextView eventDateTime = findViewById(R.id.event_date_time);
        eventDateTime.setText(date + "\n" + time);

        // Location
        TextView eventLocation = findViewById(R.id.event_location);
        eventLocation.setText(location);

        // Organizer
        TextView eventOrganizer = findViewById(R.id.event_organizer);
        eventOrganizer.setText(organizer);

        // About event
        TextView eventDescription = findViewById(R.id.event_description);
        eventDescription.setText(description);
    }

    private void setupActionButtons() {
        // Register button
        Button registerButton = findViewById(R.id.register_button);
        registerButton.setOnClickListener(v -> {
            Toast.makeText(this, "Registration functionality coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Ask button
        Button askButton = findViewById(R.id.ask_button);
        askButton.setOnClickListener(v -> {
            Toast.makeText(this, "Ask functionality coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Follow organizer button
        Button followButton = findViewById(R.id.follow_organizer_button);
        followButton.setOnClickListener(v -> {
            Toast.makeText(this, "Follow functionality coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
}

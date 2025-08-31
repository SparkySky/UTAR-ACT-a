package com.meow.utaract;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class EventDetailActivity extends AppCompatActivity {

    private Event event;
    private GuestProfile organizerProfile;
    private GuestProfile userProfile;
    private GuestProfileStorage profileStorage;
    private Button followButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Retrieve the Event and Organizer objects passed from the adapter
        event = (Event) getIntent().getSerializableExtra("event_details");
        organizerProfile = (GuestProfile) getIntent().getSerializableExtra("organizer_details");

        if (event == null) {
            Toast.makeText(this, "Error: Event not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        profileStorage = new GuestProfileStorage(this);
        userProfile = profileStorage.loadProfile();

        // Initialize views
        ImageView eventPosterImage = findViewById(R.id.event_poster_image);
        TextView eventTitleText = findViewById(R.id.event_title_text);
        TextView eventDateTimeText = findViewById(R.id.event_date_time_text);
        TextView eventLocationText = findViewById(R.id.event_location_text);
        CircleImageView organizerAvatarImage = findViewById(R.id.organizer_avatar_image);
        TextView organizerNameText = findViewById(R.id.organizer_name_text);
        TextView eventDescriptionText = findViewById(R.id.event_description_text);
        followButton = findViewById(R.id.follow_button);

        // Populate views with event data
        Glide.with(this).load(event.getCoverImageUrl()).placeholder(R.drawable.event_banner_placeholder).into(eventPosterImage);
        eventTitleText.setText(event.getEventName());
        eventDateTimeText.setText(event.getDate() + ", " + event.getTime());
        eventLocationText.setText(event.getLocation());
        eventDescriptionText.setText(event.getDescription());

        // Populate organizer details from the GuestProfile object
        if (organizerProfile != null) {
            organizerNameText.setText(organizerProfile.getName());
            Glide.with(this)
                    .load(organizerProfile.getProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .into(organizerAvatarImage);
        } else {
            organizerNameText.setText("Unknown Organizer");
            organizerAvatarImage.setImageResource(R.drawable.ic_person);
        }

        updateFollowButtonState();
        followButton.setOnClickListener(v -> toggleFollowStatus());
    }

    private void updateFollowButtonState() {
        if (userProfile != null && userProfile.getFollowing() != null && userProfile.getFollowing().contains(event.getOrganizerId())) {
            followButton.setText("Following");
        } else {
            followButton.setText("Follow");
        }
    }

    private void toggleFollowStatus() {
        if (userProfile == null) return;

        List<String> followingList = userProfile.getFollowing();
        if (followingList == null) {
            followingList = new java.util.ArrayList<>();
        }
        String organizerId = event.getOrganizerId();
        String organizerName = (organizerProfile != null) ? organizerProfile.getName() : "the organizer";

        if (followingList.contains(organizerId)) {
            followingList.remove(organizerId);
            Toast.makeText(this, "Unfollowed " + organizerName, Toast.LENGTH_SHORT).show();
        } else {
            followingList.add(organizerId);
            Toast.makeText(this, "Followed " + organizerName, Toast.LENGTH_SHORT).show();
        }

        userProfile.setFollowing(followingList);
        profileStorage.saveProfile(userProfile);

        // Also upload the updated profile to Firestore if the current user is an organizer
        boolean isCurrentUserOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);
        if (isCurrentUserOrganiser) {
            profileStorage.uploadProfileToFirestore(userProfile);
        }

        updateFollowButtonState();
    }
}
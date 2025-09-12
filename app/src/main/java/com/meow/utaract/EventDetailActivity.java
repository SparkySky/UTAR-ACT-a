package com.meow.utaract;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.util.Objects;

public class EventDetailActivity extends AppCompatActivity {

    private Event event;
    private GuestProfile organizerProfile;
    private GuestProfile userProfile;
    //private GuestProfileStorage profileStorage;
    private Button followButton, registerButton;
    private FloatingActionButton qrCodeButton, askButton;
    private ProgressBar buttonProgressBar, pageProgressBar;
    private ScrollView contentScrollView;
    private RelativeLayout registrationLayout;
    private String registrationStatus = "not_registered";
    private String eventId;
    private Bitmap qrCodeToSave;
    private LinearLayout catalogueImagesLayout;
    private LinearLayout catalogueSection;
    private TextView tvOrganiser;
    private FirebaseFirestore db;
    private GuestProfileStorage guestProfileStorage;

    // Permission Launcher: Request storage permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action.
                    if (qrCodeToSave != null) {
                        saveQrCodeToGallery(qrCodeToSave);
                    }
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied.
                    Toast.makeText(this, "Permission denied. Cannot save QR Code.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        profileStorage = new GuestProfileStorage(this);
        userProfile = profileStorage.loadProfile();

        fetchEventDetails();
    }

    private void initializeViews() {
        followButton = findViewById(R.id.follow_button);
        registerButton = findViewById(R.id.registerButton);
        qrCodeButton = findViewById(R.id.qrCodeButton);
        askButton = findViewById(R.id.askButton);
        buttonProgressBar = findViewById(R.id.buttonProgressBar);
        pageProgressBar = findViewById(R.id.pageProgressBar);
        contentScrollView = findViewById(R.id.contentScrollView);
        registrationLayout = findViewById(R.id.registration_layout);
        catalogueImagesLayout = findViewById(R.id.catalogueImagesLayout);
        catalogueSection = findViewById(R.id.catalogueSection);
    }

    private void fetchEventDetails() {
        pageProgressBar.setVisibility(View.VISIBLE);
        contentScrollView.setVisibility(View.INVISIBLE);
        registrationLayout.setVisibility(View.INVISIBLE);

        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            populateViews();
                            fetchOrganizerProfile();
                            setupListeners();
                            checkRegistrationStatus();
                        } else {
                            showErrorAndFinish("Failed to parse event data.");
                        }
                    } else {
                        showErrorAndFinish("Event not found.");
                    }
                })
                .addOnFailureListener(e -> showErrorAndFinish("Failed to load event: " + e.getMessage()));
    }

    private void fetchOrganizerProfile() {
        // ...
        FirebaseFirestore.getInstance().collection("guest_profiles").document(event.getOrganizerId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // --- THIS IS THE FIX ---
                        // Use the standard toObject() method to deserialize the profile
                        organizerProfile = documentSnapshot.toObject(GuestProfile.class);
                    }
                    displayOrganizerInfo();
                })
                .addOnFailureListener(e -> {
                    displayOrganizerInfo();
                });
        // ----------------------
    }

    private void toggleFollowStatus() {
        if (userProfile == null) return;

        String organizerId = event.getOrganizerId();
        String organizerName = (organizerProfile != null) ? organizerProfile.getName() : "the organizer";

        // Check if current user is trying to follow themselves
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(organizerId)) {
            Toast.makeText(this, "You cannot follow yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        GuestProfileStorage storage = new GuestProfileStorage(this);

        if (userProfile.getFollowing() != null && userProfile.getFollowing().contains(organizerId)) {
            // Unfollow
            userProfile.removeFollowing(organizerId);
            Toast.makeText(this, "Unfollowed " + organizerName, Toast.LENGTH_SHORT).show();
        } else {
            // Follow
            userProfile.addFollowing(organizerId);
            Toast.makeText(this, "Followed " + organizerName, Toast.LENGTH_SHORT).show();
        }

        // Save locally (guests don't save to Firestore)
        storage.saveProfile(userProfile);

        // If user is organizer, also update Firestore
        if (currentUser != null && !currentUser.isAnonymous()) {
            storage.uploadProfileToFirestore(userProfile);
        }

        updateFollowButtonState();
    }

    private void populateViews() {
        ImageView eventPosterImage = findViewById(R.id.event_poster_image);
        TextView eventTitleText = findViewById(R.id.event_title_text);
        TextView eventDateTimeText = findViewById(R.id.event_date_time_text);
        TextView eventLocationText = findViewById(R.id.event_location_text);
        TextView eventDescriptionText = findViewById(R.id.event_description_text);

        Glide.with(this)
                .load(event.getCoverImageUrl())
                .override(Target.SIZE_ORIGINAL)
                .placeholder(R.drawable.event_banner_placeholder)
                .into(eventPosterImage);
        eventTitleText.setText(event.getEventName());
        eventDateTimeText.setText(String.format("%s\n%s", event.getDate(), event.getTime()));
        eventLocationText.setText(event.getLocation());
        eventDescriptionText.setText(event.getDescription());
        populateCatalogueImages();
    }

    private void displayOrganizerInfo() {
        CircleImageView organizerAvatarImage = findViewById(R.id.organizer_avatar_image);
        TextView organizerNameText = findViewById(R.id.organizer_name_text);

        if (organizerProfile != null) {
            organizerNameText.setText(organizerProfile.getName());

            Glide.with(this)
                    .load(organizerProfile.getProfileImageUrl())
                    .override(Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.ic_person)
                    .into(organizerAvatarImage);
        } else {
            organizerNameText.setText("Unknown Organizer");
            organizerAvatarImage.setImageResource(R.drawable.ic_person);
        }
        updateFollowButtonState();

        // Show the content now that everything is ready
        pageProgressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
        registrationLayout.setVisibility(View.VISIBLE);
    }

    private void setupListeners() {
        followButton.setOnClickListener(v -> toggleFollowStatus());
        qrCodeButton.setOnClickListener(v -> generateAndShowQrCode());
        registerButton.setOnClickListener(v -> handleRegistrationClick());
        askButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.meow.utaract.chat.ChatActivity.class);
            intent.putExtra("MODE", "EVENT");
            intent.putExtra("EVENT_ID", event.getEventId());
            startActivity(intent);
        });
    }

    private void populateCatalogueImages() {
        catalogueImagesLayout.removeAllViews();
        if (event.getAdditionalImageUrls() != null && !event.getAdditionalImageUrls().isEmpty()) {
            catalogueSection.setVisibility(View.VISIBLE);

            for (String imageUrl : event.getAdditionalImageUrls()) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
                params.setMarginEnd(16);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                Glide.with(this)
                        .load(imageUrl)
                        .override(Target.SIZE_ORIGINAL)
                        .into(imageView);

                imageView.setOnClickListener(v -> {
                    FullScreenImageDialogFragment dialog = FullScreenImageDialogFragment.newInstance(imageUrl);
                    dialog.show(getSupportFragmentManager(), "FullScreenImageDialog");
                });
                catalogueImagesLayout.addView(imageView);
            }
        }
        else {
            catalogueSection.setVisibility(View.GONE);
        }
    }

    private void checkRegistrationStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            updateRegistrationButtonUI();
            return;
        }

        buttonProgressBar.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.INVISIBLE);

        FirebaseFirestore.getInstance()
                .collection("events").document(event.getEventId())
                .collection("registrations").document(currentUser.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        registrationStatus = "not_registered";
                    } else if (snapshot != null && snapshot.exists()) {
                        registrationStatus = snapshot.getString("status");
                    } else {
                        registrationStatus = "not_registered";
                    }
                    updateRegistrationButtonUI();
                });
    }

    private void updateRegistrationButtonUI() {
        buttonProgressBar.setVisibility(View.GONE);
        registerButton.setVisibility(View.VISIBLE);
        String currentStatus = registrationStatus != null ? registrationStatus : "not_registered";

        switch (registrationStatus) {
            case "pending":
                registerButton.setText("Pending\n(Click to Cancel)");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_pending_background));
                registerButton.setEnabled(true);
                break;
            case "accepted":
                registerButton.setText("Accepted");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_confirmed_background));
                registerButton.setEnabled(false); // User cannot cancel once confirmed
                break;
            case "rejected":
                registerButton.setText("Rejected");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected_background));
                registerButton.setEnabled(false); // User cannot re-register
                break;
            default: // not_registered
                registerButton.setText("Register Now");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
                registerButton.setEnabled(true);
                break;
        }
    }

    private void handleRegistrationClick() {
        if ("pending".equals(registrationStatus)) {
            showCancelConfirmationDialog();
        } else if ("not_registered".equals(registrationStatus)) {
            Intent intent = new Intent(this, GuestFormActivity.class);
            intent.putExtra("IS_REGISTRATION_MODE", true);
            intent.putExtra("EVENT_ID", event.getEventId());
            startActivity(intent);
        }
        // Do nothing if status is "accepted" or "rejected"
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Registration")
                .setMessage("Are you sure you want to cancel your registration for this event?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelRegistration())
                .setNegativeButton("No", null)
                .show();
    }

    // Cancel registered event
    private void cancelRegistration() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("events").document(event.getEventId())
                .collection("registrations").document(currentUser.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Registration cancelled.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to cancel registration.", Toast.LENGTH_SHORT).show());
    }

    // Display QR code
    private void generateAndShowQrCode() {
        String deepLink = "https://utaract.page.link/event?id=" + event.getEventId();
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bmp);

            // --- Dialog updated to remove "Share URL" button ---
            new AlertDialog.Builder(this)
                    .setTitle("Event QR Code")
                    .setView(imageView)
                    .setPositiveButton("Save", (dialog, which) -> initiateSaveProcess(bmp))
                    .setNegativeButton("Close", null)
                    .show();

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not generate QR code.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initiateSaveProcess(Bitmap bitmap) {
        this.qrCodeToSave = bitmap; // Store the bitmap in a class variable

        // Check if we are on an older version of Android that requires the permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // If we don't have permission, launch the request
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            // If we already have permission, or are on a newer Android version, save directly
            saveQrCodeToGallery(bitmap);
        }
    }

    private void saveQrCodeToGallery(Bitmap bitmap) {
        String fileName = "UTAR_ACT_Event_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            // Specify the sub-directory within Pictures for modern Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UTAR-ACT");
            }

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            fos = getContentResolver().openOutputStream(Objects.requireNonNull(imageUri));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Objects.requireNonNull(fos).close();

            Toast.makeText(this, "QR Code saved to Gallery", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save QR Code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateFollowButtonState() {
        if (userProfile != null && userProfile.getFollowing() != null &&
                userProfile.getFollowing().contains(event.getOrganizerId())) {
            followButton.setText("Following");
        } else {
            followButton.setText("Follow");
        }

        // Hide follow button if user is trying to follow themselves
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(event.getOrganizerId())) {
            followButton.setVisibility(View.GONE);
        } else {
            followButton.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
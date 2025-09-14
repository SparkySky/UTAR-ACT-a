package com.meow.utaract.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.meow.utaract.R;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import com.meow.utaract.utils.News;
import com.meow.utaract.utils.NewsStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NewsCreationActivity extends AppCompatActivity {

    // UI components
    private EditText titleInput, messageInput;
    private Button btnCreate, btnUploadImage;
    private LinearLayout newsImagePreviewLayout;

    // Lists to store selected images and uploaded image URLs
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();

    // Storage handler for saving news to Firestore
    private NewsStorage newsStorage;

    // Flags and data for edit mode
    private boolean isEditMode = false;
    private News newsToEdit;

    // Callback interface to fetch organizer's name asynchronously
    interface OrganizerNameCallback {
        void onSuccess(String organizerName);
        void onFailure(Exception e);
    }

    // Launcher for picking multiple or single images
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        // Multiple images selected
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count && selectedImageUris.size() < 5; i++) {
                            selectedImageUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        // Single image selected
                        if (selectedImageUris.size() < 5) {
                            selectedImageUris.add(result.getData().getData());
                        }
                    }
                    updateImagePreviews(); // Refresh image previews after selection
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_creation);

        initializeViews(); // Bind UI components and set listeners
        newsStorage = new NewsStorage();

        // If intent contains EDIT_NEWS, switch to edit mode
        if (getIntent().hasExtra("EDIT_NEWS")) {
            isEditMode = true;
            newsToEdit = (News) getIntent().getSerializableExtra("EDIT_NEWS");
            populateEditData(); // Load existing news data for editing
        }
    }

    /**
     * Initialize UI elements and set button click listeners
     */
    private void initializeViews() {
        titleInput = findViewById(R.id.newsTitleInput);
        messageInput = findViewById(R.id.newsMessageInput);
        btnCreate = findViewById(R.id.btnCreateNews);
        btnUploadImage = findViewById(R.id.btnUploadNewsImages);
        newsImagePreviewLayout = findViewById(R.id.newsImagePreviewLayout);

        btnUploadImage.setOnClickListener(v -> openImagePicker());
        btnCreate.setOnClickListener(v -> saveNews());
    }

    /**
     * Open system image picker to select single or multiple images
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple selection
        imagePickerLauncher.launch(intent);
    }

    /**
     * Dynamically generate previews of selected images
     * Each preview includes a remove button
     */
    private void updateImagePreviews() {
        newsImagePreviewLayout.removeAllViews();
        newsImagePreviewLayout.setVisibility(View.VISIBLE);

        for (Uri uri : selectedImageUris) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
            params.setMarginEnd(8);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Button to remove image from list
            ImageView removeBtn = new ImageView(this);
            removeBtn.setLayoutParams(new LinearLayout.LayoutParams(30, 30));
            removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            removeBtn.setOnClickListener(v -> {
                selectedImageUris.remove(uri);
                updateImagePreviews();
            });

            // Add image and remove button to container
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(imageView);
            container.addView(removeBtn);

            newsImagePreviewLayout.addView(container);

            // Load image using Glide
            Glide.with(this)
                    .load(uri)
                    .override(Target.SIZE_ORIGINAL)
                    .into(imageView);
        }
    }

    /**
     * Validate input and either create/update news directly
     * or upload images first
     */
    private void saveNews() {
        String title = titleInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill in title and message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            createOrUpdateNews(new ArrayList<>()); // No images
        } else {
            uploadImagesAndSave(); // Upload images before saving news
        }
    }

    /**
     * Upload selected images to Firebase Storage
     * and then proceed with news creation
     */
    private void uploadImagesAndSave() {
        uploadedImageUrls.clear();
        List<StorageReference> uploadTasks = new ArrayList<>();

        for (Uri uri : selectedImageUris) {
            String fileName = "news_images/" + UUID.randomUUID().toString();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);
            uploadTasks.add(storageRef);

            storageRef.putFile(uri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                uploadedImageUrls.add(downloadUri.toString());
                                // Once all images are uploaded, create news
                                if (uploadedImageUrls.size() == selectedImageUris.size()) {
                                    createOrUpdateNews(uploadedImageUrls);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Fetch organizer's name and then proceed to save or update news
     */
    private void createOrUpdateNews(List<String> imageUrls) {
        String organizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // First try to fetch organizer name from Firestore
        fetchOrganizerNameFromFirebase(organizerId, new OrganizerNameCallback() {
            @Override
            public void onSuccess(String organizerName) {
                proceedWithNewsCreation(organizerId, organizerName, imageUrls);
            }

            @Override
            public void onFailure(Exception e) {
                // Fallback: use locally saved profile
                GuestProfileStorage profileStorage = new GuestProfileStorage(NewsCreationActivity.this);
                GuestProfile organizerProfile = profileStorage.loadProfile();
                String organizerName = organizerProfile != null ? organizerProfile.getName() : "Unknown Organizer";
                proceedWithNewsCreation(organizerId, organizerName, imageUrls);
            }
        });
    }

    /**
     * Query Firestore for organizer's profile
     */
    private void fetchOrganizerNameFromFirebase(String organizerId, OrganizerNameCallback callback) {
        FirebaseFirestore.getInstance().collection("guest_profiles").document(organizerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String json = documentSnapshot.getString("profile_json");
                        if (json != null) {
                            GuestProfile profile = new Gson().fromJson(json, GuestProfile.class);
                            callback.onSuccess(profile.getName());
                        } else {
                            callback.onFailure(new Exception("Profile JSON not found"));
                        }
                    } else {
                        callback.onFailure(new Exception("Organizer profile not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Final step: either create new news or update existing one
     */
    private void proceedWithNewsCreation(String organizerId, String organizerName, List<String> imageUrls) {
        if (isEditMode && newsToEdit != null) {
            // Update existing news
            newsToEdit.setTitle(titleInput.getText().toString());
            newsToEdit.setMessage(messageInput.getText().toString());
            newsToEdit.setImageUrls(imageUrls);
            newsToEdit.setUpdatedAt(System.currentTimeMillis());
            newsToEdit.setOrganizerName(organizerName);

            newsStorage.updateNews(newsToEdit.getNewsId(), newsToEdit, new NewsStorage.NewsCallback() {
                @Override
                public void onSuccess(String newsId) {
                    Toast.makeText(NewsCreationActivity.this, "News updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(NewsCreationActivity.this, "Failed to update news: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Create new news
            News news = new News(organizerId, organizerName, titleInput.getText().toString(), messageInput.getText().toString());
            news.setImageUrls(imageUrls);

            newsStorage.createNews(news, new NewsStorage.NewsCallback() {
                @Override
                public void onSuccess(String newsId) {
                    Toast.makeText(NewsCreationActivity.this, "News created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(NewsCreationActivity.this, "Failed to create news: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Fill UI with data when editing existing news
     */
    private void populateEditData() {
        if (newsToEdit != null) {
            titleInput.setText(newsToEdit.getTitle());
            messageInput.setText(newsToEdit.getMessage());
            btnCreate.setText("Update News");

            // Load existing images
            uploadedImageUrls = new ArrayList<>(newsToEdit.getImageUrls());
            updateImagePreviews();

            // Show delete button in edit mode
            Button deleteButton = findViewById(R.id.btnDeleteNews);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        }
    }

    /**
     * Show a confirmation dialog before deleting news
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete News")
                .setMessage("Are you sure you want to delete this news?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNews())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete selected news from Firestore
     */
    private void deleteNews() {
        NewsStorage newsStorage = new NewsStorage();
        newsStorage.deleteNews(newsToEdit.getNewsId(), new NewsStorage.NewsCallback() {
            @Override
            public void onSuccess(String newsId) {
                runOnUiThread(() -> {
                    Toast.makeText(NewsCreationActivity.this, "News deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewsCreationActivity.this, "Failed to delete news: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}

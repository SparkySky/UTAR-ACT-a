package com.meow.utaract;

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
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import com.meow.utaract.utils.News;
import com.meow.utaract.utils.NewsStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NewsCreationActivity extends AppCompatActivity {

    private EditText titleInput, messageInput;
    private Button btnCreate, btnUploadImage;
    private LinearLayout newsImagePreviewLayout;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();
    private NewsStorage newsStorage;
    private boolean isEditMode = false;
    private News newsToEdit;

    interface OrganizerNameCallback {
        void onSuccess(String organizerName);
        void onFailure(Exception e);
    }

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
                    updateImagePreviews();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_creation);

        initializeViews();
        newsStorage = new NewsStorage();

        if (getIntent().hasExtra("EDIT_NEWS")) {
            isEditMode = true;
            newsToEdit = (News) getIntent().getSerializableExtra("EDIT_NEWS");
            populateEditData();
        }
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.newsTitleInput);
        messageInput = findViewById(R.id.newsMessageInput);
        btnCreate = findViewById(R.id.btnCreateNews);
        btnUploadImage = findViewById(R.id.btnUploadNewsImages);
        newsImagePreviewLayout = findViewById(R.id.newsImagePreviewLayout);

        btnUploadImage.setOnClickListener(v -> openImagePicker());
        btnCreate.setOnClickListener(v -> saveNews());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(intent);
    }

    private void updateImagePreviews() {
        newsImagePreviewLayout.removeAllViews();
        newsImagePreviewLayout.setVisibility(View.VISIBLE);
        for (Uri uri : selectedImageUris) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
            params.setMarginEnd(8);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            ImageView removeBtn = new ImageView(this);
            removeBtn.setLayoutParams(new LinearLayout.LayoutParams(30, 30));
            removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            removeBtn.setOnClickListener(v -> {
                selectedImageUris.remove(uri);
                updateImagePreviews();
            });

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(imageView);
            container.addView(removeBtn);

            newsImagePreviewLayout.addView(container);
            Glide.with(this)
                    .load(uri)
                    .override(Target.SIZE_ORIGINAL)
                    .into(imageView);
        }
    }

    private void saveNews() {
        String title = titleInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill in title and message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            createOrUpdateNews(new ArrayList<>());
        } else {
            uploadImagesAndSave();
        }
    }

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
                                if (uploadedImageUrls.size() == selectedImageUris.size()) {
                                    createOrUpdateNews(uploadedImageUrls);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void createOrUpdateNews(List<String> imageUrls) {
        String organizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch organizer name from Firebase instead of local storage
        fetchOrganizerNameFromFirebase(organizerId, new OrganizerNameCallback() {
            @Override
            public void onSuccess(String organizerName) {
                proceedWithNewsCreation(organizerId, organizerName, imageUrls);
            }

            @Override
            public void onFailure(Exception e) {
                // Fallback to local storage if Firebase fails
                GuestProfileStorage profileStorage = new GuestProfileStorage(NewsCreationActivity.this);
                GuestProfile organizerProfile = profileStorage.loadProfile();
                String organizerName = organizerProfile != null ? organizerProfile.getName() : "Unknown Organizer";
                proceedWithNewsCreation(organizerId, organizerName, imageUrls);
            }
        });
    }

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

    private void proceedWithNewsCreation(String organizerId, String organizerName, List<String> imageUrls) {
        if (isEditMode && newsToEdit != null) {
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

    private void populateEditData() {
        if (newsToEdit != null) {
            titleInput.setText(newsToEdit.getTitle());
            messageInput.setText(newsToEdit.getMessage());
            btnCreate.setText("Update News");

            // Load existing images
            uploadedImageUrls = new ArrayList<>(newsToEdit.getImageUrls());
            updateImagePreviews();

            // If editing, show delete button
            Button deleteButton = findViewById(R.id.btnDeleteNews);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete News")
                .setMessage("Are you sure you want to delete this news?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNews())
                .setNegativeButton("Cancel", null)
                .show();
    }

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
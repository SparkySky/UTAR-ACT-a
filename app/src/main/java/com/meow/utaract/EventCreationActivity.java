package com.meow.utaract;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;

public class EventCreationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_COVER_IMAGE = 1;
    private static final int PICK_ADDITIONAL_IMAGES = 2;

    private TextInputEditText etDate, etTime;
    private ImageView ivCoverPreview;
    private LinearLayout layoutPicturesPreview;
    private Spinner spinnerCategory;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_creation);

        initializeViews();
        setupNavigationDrawer();
        setupCategorySpinner();
        setupDateTimePickers();
        setupButtonListeners();
    }

    private void initializeViews() {
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        ivCoverPreview = findViewById(R.id.ivCoverPreview);
        layoutPicturesPreview = findViewById(R.id.layoutPicturesPreview);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        drawerLayout = findViewById(R.id.drawer_layout);
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up toolbar with navigation icon
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
    }

    private void setupCategorySpinner() {
        String[] categories = {
                getString(R.string.pref_music_concert),
                getString(R.string.pref_sports_competition),
                getString(R.string.pref_volunteering),
                getString(R.string.pref_workshops),
                getString(R.string.pref_exhibition),
                getString(R.string.pref_seminar),
                getString(R.string.pref_networking),
                getString(R.string.pref_cultural_fest),
                getString(R.string.pref_talk)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupDateTimePickers() {
        final Calendar calendar = Calendar.getInstance();

        // Date Picker
        etDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    EventCreationActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        etDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Time Picker
        etTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    EventCreationActivity.this,
                    (view, hourOfDay, minute) -> {
                        String selectedTime = String.format("%02d:%02d", hourOfDay, minute);
                        etTime.setText(selectedTime);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });
    }

    private void setupButtonListeners() {
        // Cover Upload
        findViewById(R.id.btnUploadCover).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_COVER_IMAGE);
        });

        // Additional Pictures Upload
        findViewById(R.id.btnUploadPictures).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_ADDITIONAL_IMAGES);
        });

        // Reset Button
        findViewById(R.id.btnReset).setOnClickListener(v -> resetForm());

        // Create Button
        findViewById(R.id.btnCreate).setOnClickListener(v -> createEvent());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_COVER_IMAGE) {
                Uri imageUri = data.getData();
                ivCoverPreview.setImageURI(imageUri);
                ivCoverPreview.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_ADDITIONAL_IMAGES) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        addImagePreview(imageUri);
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    addImagePreview(imageUri);
                }
            }
        }
    }

    private void addImagePreview(Uri imageUri) {
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
        params.setMargins(0, 0, 16, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(imageUri);
        layoutPicturesPreview.addView(imageView);
    }

    private void resetForm() {
        ((TextInputEditText) findViewById(R.id.etEventName)).setText("");
        ((TextInputEditText) findViewById(R.id.etDescription)).setText("");
        etDate.setText("");
        etTime.setText("");
        ((TextInputEditText) findViewById(R.id.etLocation)).setText("");
        spinnerCategory.setSelection(0);

        ivCoverPreview.setVisibility(View.GONE);
        layoutPicturesPreview.removeAllViews();

        Toast.makeText(this, "Form reset", Toast.LENGTH_SHORT).show();
    }

    private void createEvent() {
        String eventName = ((TextInputEditText) findViewById(R.id.etEventName)).getText().toString();
        String description = ((TextInputEditText) findViewById(R.id.etDescription)).getText().toString();
        String date = etDate.getText().toString();
        String time = etTime.getText().toString();
        String location = ((TextInputEditText) findViewById(R.id.etLocation)).getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();

        if (eventName.isEmpty() || description.isEmpty() || date.isEmpty() ||
                time.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle home navigation
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_gallery) {
            // Handle gallery navigation
        } else if (id == R.id.nav_slideshow) {
            // Handle slideshow navigation
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
package com.meow.utaract.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.meow.utaract.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * TicketActivity handles displaying ticket details (event, attendee info, QR code)
 * and saving the ticket as an image into the device storage.
 */
public class TicketActivity extends AppCompatActivity {

    private View ticketLayout; // The whole ticket layout to capture as image
    private String eventName;  // Event name
    private String ticketCode; // Ticket verification code

    // Request permission launcher for WRITE_EXTERNAL_STORAGE (for Android < Q)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted → Save ticket
                    saveTicket();
                } else {
                    // Permission denied → Show warning
                    Toast.makeText(this, "Permission denied. Cannot save ticket.", Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Called when the activity is created.
     * Initializes views, loads ticket data, generates QR code, and sets up save button.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        // Initialize layout references
        ticketLayout = findViewById(R.id.ticketLayout);
        Button saveTicketButton = findViewById(R.id.saveTicketButton);

        // Toolbar back button → closes activity
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // Retrieve ticket data passed via Intent
        eventName = getIntent().getStringExtra("EVENT_NAME");
        ticketCode = getIntent().getStringExtra("TICKET_CODE");
        String attendeeName = getIntent().getStringExtra("ATTENDEE_NAME");
        String attendeeEmail = getIntent().getStringExtra("ATTENDEE_EMAIL");
        String attendeePhone = getIntent().getStringExtra("ATTENDEE_PHONE");

        // Populate UI with ticket details
        ((TextView) findViewById(R.id.eventNameText)).setText(eventName);
        ((TextView) findViewById(R.id.verificationCodeText)).setText("Verification Code: " + ticketCode);
        ((TextView) findViewById(R.id.attendeeNameText)).setText(attendeeName);
        ((TextView) findViewById(R.id.attendeeEmailText)).setText(attendeeEmail);
        ((TextView) findViewById(R.id.attendeePhoneText)).setText(attendeePhone);

        // Generate and display QR code
        ImageView qrCodeImageView = findViewById(R.id.qrCodeImageView);
        try {
            Bitmap qrBitmap = generateQrCode(ticketCode);
            qrCodeImageView.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        // Save button → request permission and save ticket
        saveTicketButton.setOnClickListener(v -> checkPermissionAndSave());
    }

    /**
     * Checks storage permission before saving.
     * - For Android Q and above, no permission required.
     * - For Android < Q, request WRITE_EXTERNAL_STORAGE permission if not granted.
     */
    private void checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (Scoped Storage) → No explicit permission needed
            saveTicket();
        } else {
            // Older versions → Check WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                saveTicket();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * Captures the ticket layout as a bitmap and saves it as a JPEG image.
     * - For Android Q and above: saves via MediaStore.
     * - For Android < Q: saves to external storage directory.
     */
    private void saveTicket() {
        // Convert ticket layout into a Bitmap
        Bitmap bitmap = Bitmap.createBitmap(ticketLayout.getWidth(), ticketLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        ticketLayout.draw(canvas);

        // File name for the saved ticket image
        String filename = "Ticket-" + eventName.replaceAll("[^a-zA-Z0-9]", "") + "-" + ticketCode + ".jpg";

        try {
            OutputStream fos;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Save using MediaStore (Scoped Storage)
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UTARACT Tickets");

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                // Save to external storage manually
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/UTARACT Tickets";
                File dir = new File(imagesDir);
                if (!dir.exists()) {
                    dir.mkdirs(); // Create folder if it doesn't exist
                }
                File imageFile = new File(dir, filename);
                fos = new FileOutputStream(imageFile);
            }

            // Compress and write bitmap to output stream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            // Show success message
            Toast.makeText(this, "Ticket saved to Pictures/UTARACT Tickets", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving ticket: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Generates a QR code bitmap based on the provided text.
     *
     * @param text The text to encode into QR code.
     * @return Bitmap containing QR code.
     * @throws WriterException if QR code generation fails.
     */
    private Bitmap generateQrCode(String text) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        // Fill the QR code bitmap (black and white pixels)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }

        return bmp;
    }
}

package com.meow.utaract.chat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.meow.utaract.AiService;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText chatInput;
    private Button sendButton;

    private String mode; // GENERAL or EVENT
    private String eventId;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatContainer = findViewById(R.id.chat_container);
        chatInput = findViewById(R.id.chat_input);
        sendButton = findViewById(R.id.send_button);

        mode = getIntent().getStringExtra("MODE");
        eventId = getIntent().getStringExtra("EVENT_ID");

        db = FirebaseFirestore.getInstance();

        sendButton.setOnClickListener(v -> onSend());

        // Set up toolbar navigation
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> showExitDialog());
        
        // Add welcome message
        if ("EVENT".equals(mode)) {
            appendBubble("Hi! I can help you with questions about this specific event. What would you like to know?", false);
        } else {
            appendBubble("Hi! I'm your UTARACT assistant. I can help you find events that match your interests and preferences. What kind of activities are you looking for?", false);
        }
    }

    private void onSend() {
        String userMessage = chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(userMessage)) return;
        appendBubble(userMessage, true);
        chatInput.setText("");
        
        // Show loading indicator
        appendBubble("Thinking...", false);

        String apiKey = getString(R.string.gemini_api_key);
        AiService ai = new AiService(apiKey);

        if ("EVENT".equals(mode) && !TextUtils.isEmpty(eventId)) {
            db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
                Event event = doc.toObject(Event.class);
                String context = buildEventContext(event);
                ai.chat(context, userMessage, new UiCallback());
            });
        } else {
            db.collection("events").get().addOnSuccessListener(qs -> {
                List<Event> events = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot d : qs.getDocuments()) {
                    Event e = d.toObject(Event.class);
                    if (e != null) events.add(e);
                }
                String context = buildGeneralContext(events);
                ai.chat(context, userMessage, new UiCallback());
            });
        }
    }

    private String buildEventContext(@Nullable Event e) {
        if (e == null) return "You are UTARACT assistant. Event not found.";
        String desc = e.getSummary() != null && !e.getSummary().isEmpty() ? e.getSummary() : e.getDescription();
        
        StringBuilder context = new StringBuilder();
        context.append("You are UTARACT event assistant. Answer questions about this specific event. Be helpful and concise.\n\n");
        context.append("EVENT DETAILS:\n");
        context.append("Title: ").append(e.getEventName()).append("\n");
        context.append("Date & Time: ").append(e.getDate()).append(" at ").append(e.getTime()).append("\n");
        context.append("Location: ").append(e.getLocation()).append("\n");
        context.append("Category: ").append(e.getCategory()).append("\n");
        context.append("Fee: RM").append(e.getFee()).append(e.getFee() == 0 ? " (Free)" : "").append("\n");
        context.append("Max Guests: ").append(e.getMaxGuests()).append("\n\n");
        context.append("EVENT SUMMARY:\n").append(desc).append("\n\n");
        
        // Include uploaded document text if available
        if (e.getUploadedDocumentText() != null && !e.getUploadedDocumentText().isEmpty()) {
            String docName = e.getUploadedDocumentName() != null ? e.getUploadedDocumentName() : "Uploaded Document";
            context.append("UPLOADED DOCUMENT (").append(docName).append(") CONTENT:\n");
            context.append(e.getUploadedDocumentText()).append("\n\n");
            context.append("You can reference the uploaded document content to provide more detailed answers about the event.\n\n");
        }
        
        context.append("Answer user questions about this event. If asked about other events, politely redirect to the general chat.");
        return context.toString();
    }

    private String buildGeneralContext(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are UTARACT assistant. Help users find suitable events based on their preferences and needs. " +
                "Consider categories, dates, locations, fees, and event descriptions. Be friendly and helpful.\n\n");
        sb.append("AVAILABLE EVENTS:\n");
        int count = 0;
        for (Event e : events) {
            if (count++ >= 15) break; // bound context
            String desc = e.getSummary() != null && !e.getSummary().isEmpty() ? e.getSummary() : e.getDescription();
            sb.append("• ").append(e.getEventName())
              .append("\n  Date: ").append(e.getDate()).append(" at ").append(e.getTime())
              .append("\n  Location: ").append(e.getLocation())
              .append("\n  Category: ").append(e.getCategory())
              .append("\n  Fee: RM").append(e.getFee()).append(e.getFee() == 0 ? " (Free)" : "")
              .append("\n  Max Guests: ").append(e.getMaxGuests())
              .append("\n  Details: ").append(desc);
            
            // Add note if document is available
            if (e.getUploadedDocumentText() != null && !e.getUploadedDocumentText().isEmpty()) {
                sb.append("\n  📄 Has uploaded document with additional details");
            }
            sb.append("\n\n");
        }
        sb.append("Recommend events based on user preferences. Ask clarifying questions if needed to provide better suggestions.");
        return sb.toString();
    }

    private class UiCallback implements AiService.AiCallback {
        @Override
        public void onSuccess(String text) { 
            runOnUiThread(() -> {
                // Remove the "Thinking..." message and add the actual response
                if (chatContainer.getChildCount() > 0) {
                    View lastChild = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
                    if (lastChild instanceof TextView) {
                        TextView lastTv = (TextView) lastChild;
                        if ("Thinking...".equals(lastTv.getText().toString())) {
                            chatContainer.removeView(lastChild);
                        }
                    }
                }
                appendBubble(text, false);
            });
        }
        @Override
        public void onError(Exception e) { 
            runOnUiThread(() -> {
                // Remove the "Thinking..." message and add error
                if (chatContainer.getChildCount() > 0) {
                    View lastChild = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
                    if (lastChild instanceof TextView) {
                        TextView lastTv = (TextView) lastChild;
                        if ("Thinking...".equals(lastTv.getText().toString())) {
                            chatContainer.removeView(lastChild);
                        }
                    }
                }
                appendBubble("Sorry, I encountered an error. Please try again.", false);
            });
        }
    }

    private void appendBubble(String text, boolean isUser) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        params.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        params.leftMargin = isUser ? (int) (48 * getResources().getDisplayMetrics().density) : (int) (12 * getResources().getDisplayMetrics().density);
        params.rightMargin = isUser ? (int) (12 * getResources().getDisplayMetrics().density) : (int) (48 * getResources().getDisplayMetrics().density);
        params.gravity = isUser ? android.view.Gravity.END : android.view.Gravity.START;
        tv.setLayoutParams(params);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setPadding(16, 12, 16, 12);
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.7));
        
        // Set proper Material Design bubble background
        if (isUser) {
            tv.setBackgroundResource(R.drawable.chat_bubble_user);
            tv.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            tv.setBackgroundResource(R.drawable.chat_bubble_bot);
            tv.setTextColor(getResources().getColor(android.R.color.black));
        }
        
        chatContainer.addView(tv);
        
        // Auto-scroll to bottom
        chatContainer.post(() -> {
            View lastChild = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
            if (lastChild != null) {
                lastChild.requestFocus();
            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Chat")
                .setMessage("Closing the chat will clear your conversation history. Are you sure you want to exit?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
    }
}




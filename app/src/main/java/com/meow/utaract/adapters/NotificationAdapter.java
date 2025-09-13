package com.meow.utaract.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.meow.utaract.utils.Notification;
import com.meow.utaract.R;
import com.meow.utaract.activities.ApplicantListActivity;
import com.meow.utaract.activities.TicketActivity;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notificationList; // Back to a simple list of objects
    private final Context context;
    private final boolean isOrganiser;

    public NotificationAdapter(List<Notification> notificationList, Context context, boolean isOrganiser) {
        this.notificationList = notificationList;
        this.context = context;
        this.isOrganiser = isOrganiser;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestampText;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.notificationMessage);
            timestampText = itemView.findViewById(R.id.notificationTimestamp);
        }

        void bind(Notification notification) {
            if (notification == null) return;

            // Message logic
            if (notification.getMessage() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    messageText.setText(Html.fromHtml(notification.getMessage(), Html.FROM_HTML_MODE_COMPACT));
                } else {
                    messageText.setText(Html.fromHtml(notification.getMessage()));
                }
            }

            // Timestamp logic is now simple
            if (notification.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                timestampText.setText(sdf.format(notification.getTimestamp()));
            } else {
                timestampText.setText("");
            }

            // ---------------------------------------------

// Click listener for Guests to view their ticket
            if (notification.getMessage() != null && notification.getMessage().startsWith("Your registration") && notification.getMessage().contains("accepted")) {
                itemView.setOnClickListener(v -> {
                    FirebaseFirestore.getInstance().collection("events").document(notification.getEventId())
                            .get().addOnSuccessListener(documentSnapshot -> {
                                Event event = documentSnapshot.toObject(Event.class);
                                if (event != null) {
                                    GuestProfile userProfile = new GuestProfileStorage(context).loadProfile();
                                    if (userProfile != null) {
                                        Intent intent = new Intent(context, TicketActivity.class);
                                        intent.putExtra("EVENT_NAME", event.getEventName());
                                        intent.putExtra("TICKET_CODE", notification.getTicketCode());
                                        intent.putExtra("ATTENDEE_NAME", userProfile.getName());
                                        intent.putExtra("ATTENDEE_EMAIL", userProfile.getEmail());
                                        intent.putExtra("ATTENDEE_PHONE", userProfile.getPhone());
                                        context.startActivity(intent);
                                    }
                                }
                            });
                });
            }
            // Click listener for Organizers to view the applicant list
            else if (notification.getMessage() != null && notification.getMessage().startsWith("New applicant")) {
                itemView.setOnClickListener(v -> {
                    if (notification.getEventId() != null && notification.getOrganizerId() != null) {
                        Intent intent = new Intent(context, ApplicantListActivity.class);
                        intent.putExtra("EVENT_ID", notification.getEventId());
                        intent.putExtra("ORGANIZER_ID", notification.getOrganizerId());
                        context.startActivity(intent);
                    }
                });
            }
            // For all other notifications, do nothing on click
            else {
                itemView.setOnClickListener(null);
            }
        }
    }
}
package com.meow.utaract.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.meow.utaract.EventCreationActivity;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.MyEventViewHolder> {

    private List<Event> eventList;
    private Context context;
    private EventCreationStorage eventStorage;

    public MyEventsAdapter(List<Event> eventList, Context context, EventCreationStorage eventStorage) {
        this.eventList = eventList;
        this.context = context;
        this.eventStorage = eventStorage;
    }

    @NonNull
    @Override
    public MyEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_my_event, parent, false);
        return new MyEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyEventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    class MyEventViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitleText, createdDateText, publishTimeText;
        MaterialSwitch visibilitySwitch;
        Button deleteButton, editButton;

        MyEventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitleText = itemView.findViewById(R.id.eventTitleText);
            createdDateText = itemView.findViewById(R.id.createdDateText);
            publishTimeText = itemView.findViewById(R.id.publishTimeText);
            visibilitySwitch = itemView.findViewById(R.id.visibilitySwitch);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
        }

        void bind(final Event event) {
            eventTitleText.setText(event.getEventName());
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            createdDateText.setText("Created on: " + sdf.format(new Date(event.getCreatedAt())));

            visibilitySwitch.setChecked(event.isVisible());

            if (event.getPublishAt() > System.currentTimeMillis() && !event.isVisible()) {
                SimpleDateFormat publishSdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
                publishTimeText.setText("Scheduled for: " + publishSdf.format(new Date(event.getPublishAt())));
                publishTimeText.setVisibility(View.VISIBLE);
            } else {
                publishTimeText.setVisibility(View.GONE);
            }

            // Set listeners without executing update logic immediately
            visibilitySwitch.setOnCheckedChangeListener(null);
            visibilitySwitch.setChecked(event.isVisible());
            visibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                event.setVisible(isChecked);
                // If they make it visible, set publish time to now
                if (isChecked) {
                    event.setPublishAt(System.currentTimeMillis());
                }
                updateEventInFirestore(event);
            });

            // Delete button logic
            deleteButton.setEnabled(isEventDeletable(event.getDate()));
            deleteButton.setOnClickListener(v -> showDeleteConfirmation(event));

            // Edit button logic
            editButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, EventCreationActivity.class);
                intent.putExtra("IS_EDIT_MODE", true);
                intent.putExtra("EDIT_EVENT_DATA", event);
                context.startActivity(intent);
            });
        }

        private boolean isEventDeletable(String eventDateStr) {
            try {
                SimpleDateFormat eventSdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                Date eventDate = eventSdf.parse(eventDateStr);
                return eventDate != null && eventDate.after(new Date());
            } catch (ParseException e) {
                return false; // Cannot parse date, disable deletion
            }
        }

        private void showDeleteConfirmation(Event event) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        eventStorage.deleteEvent(event.getEventId(), new EventCreationStorage.EventDeletionCallback() {
                            @Override
                            public void onSuccess() {
                                int currentPosition = getAdapterPosition();
                                if (currentPosition != RecyclerView.NO_POSITION) {
                                    eventList.remove(currentPosition);
                                    notifyItemRemoved(currentPosition);
                                    Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(context, "Failed to delete event", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void updateEventInFirestore(Event event) {
            eventStorage.updateEvent(event.getEventId(), event, new EventCreationStorage.EventCreationCallback() {
                @Override
                public void onSuccess(String eventId) {
                    Toast.makeText(context, "Visibility updated", Toast.LENGTH_SHORT).show();
                    bind(event); // Re-bind to update the UI state (e.g., the scheduled text)
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
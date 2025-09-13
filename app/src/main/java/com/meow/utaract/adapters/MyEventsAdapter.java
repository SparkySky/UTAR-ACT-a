package com.meow.utaract.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.meow.utaract.activities.ApplicantListActivity;
import com.meow.utaract.activities.EventCreationActivity;
import com.meow.utaract.utils.ManagedEventItem;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.MyEventViewHolder> {

    private final List<ManagedEventItem> eventItemList;
    private final Context context;
    private final EventCreationStorage eventStorage;

    public MyEventsAdapter(List<ManagedEventItem> eventItemList, Context context, EventCreationStorage eventStorage) {
        this.eventItemList = new ArrayList<>(eventItemList);
        this.context = context;
        this.eventStorage = eventStorage;
    }

    public void updateEvents(List<ManagedEventItem> newEvents) {
        this.eventItemList.clear();
        this.eventItemList.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_manage_event, parent, false);
        return new MyEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyEventViewHolder holder, int position) {
        holder.bind(eventItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return eventItemList.size();
    }

    class MyEventViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitleText, createdDateText, eventStatusText;
        Button deleteButton, editButton, viewApplicantsButton;
        LinearLayout statusIndicatorLayout;
        TextView pendingCountText, acceptedCountText, rejectedCountText;

        MyEventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitleText = itemView.findViewById(R.id.eventTitleText);
            createdDateText = itemView.findViewById(R.id.createdDateText);
            eventStatusText = itemView.findViewById(R.id.eventStatusText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
            viewApplicantsButton = itemView.findViewById(R.id.viewApplicantsButton);
            statusIndicatorLayout = itemView.findViewById(R.id.statusIndicatorLayout);
            pendingCountText = itemView.findViewById(R.id.pendingCountText);
            acceptedCountText = itemView.findViewById(R.id.acceptedCountText);
            rejectedCountText = itemView.findViewById(R.id.rejectedCountText);
        }

        void bind(final ManagedEventItem managedEventItem) {
            final Event event = managedEventItem.getEvent();
            eventTitleText.setText(event.getEventName());
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            createdDateText.setText("Created on: " + sdf.format(new Date(event.getCreatedAt())));

            if (event.getPublishAt() <= System.currentTimeMillis()) {
                eventStatusText.setText("Status: Published");
                eventStatusText.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_primary));
            } else {
                SimpleDateFormat publishSdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
                eventStatusText.setText("Status: Scheduled for " + publishSdf.format(new Date(event.getPublishAt())));
                eventStatusText.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_secondary));
            }

            boolean isEventInFuture = isEventInFuture(event.getDate());
            editButton.setEnabled(isEventInFuture);
            deleteButton.setEnabled(isEventInFuture);

            deleteButton.setOnClickListener(v -> showDeleteConfirmation(event));
            editButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, EventCreationActivity.class);
                // Pass only the unique ID of the event
                intent.putExtra("eventId", event.getEventId());
                context.startActivity(intent);
            });
            viewApplicantsButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, ApplicantListActivity.class);
                intent.putExtra("EVENT_ID", event.getEventId());
                intent.putExtra("ORGANIZER_ID", event.getOrganizerId()); // Add this line
                context.startActivity(intent);
            });

            int totalApplicants = managedEventItem.getPendingCount() + managedEventItem.getAcceptedCount() + managedEventItem.getRejectedCount();
            if (totalApplicants > 0) {
                statusIndicatorLayout.setVisibility(View.VISIBLE);
                pendingCountText.setText(String.valueOf(managedEventItem.getPendingCount()));
                acceptedCountText.setText(String.valueOf(managedEventItem.getAcceptedCount()));
                rejectedCountText.setText(String.valueOf(managedEventItem.getRejectedCount()));
            } else {
                statusIndicatorLayout.setVisibility(View.GONE);
            }
        }

        private boolean isEventInFuture(String eventDateStr) {
            try {
                SimpleDateFormat eventSdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                Date eventDate = eventSdf.parse(eventDateStr);
                Calendar eventCal = Calendar.getInstance();
                if (eventDate != null) {
                    eventCal.setTime(eventDate);
                }
                eventCal.set(Calendar.HOUR_OF_DAY, 23);
                eventCal.set(Calendar.MINUTE, 59);
                eventCal.set(Calendar.SECOND, 59);
                return eventCal.getTime().after(new Date());
            } catch (ParseException e) {
                return false;
            }
        }

        private void showDeleteConfirmation(Event event) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteEvent(event))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deleteEvent(Event event) {
            eventStorage.deleteEvent(event.getEventId(), new EventCreationStorage.EventDeletionCallback() {
                @Override
                public void onSuccess() {
                    int currentPosition = getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        eventItemList.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                        Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(context, "Failed to delete event", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
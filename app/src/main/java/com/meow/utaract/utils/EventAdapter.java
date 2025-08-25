package com.meow.utaract.utils;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meow.utaract.EventDetailsActivity;
import com.meow.utaract.R;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    
    private List<Event> events;
    private Context context;

    public EventAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        
        holder.eventTitle.setText(event.getTitle());
        holder.eventAudience.setText(event.getAudience());
        holder.eventDate.setText(event.getDate());
        holder.eventLocation.setText(event.getLocation());
        holder.dateBadge.setText(event.getDateBadge());
        holder.categoryTag.setText(event.getCategoryTag());

        // Set click listener to navigate to event details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("event_id", event.getId());
            intent.putExtra("event_title", event.getTitle());
            intent.putExtra("event_description", event.getDescription());
            intent.putExtra("event_date", event.getDate());
            intent.putExtra("event_time", event.getTime());
            intent.putExtra("event_location", event.getLocation());
            intent.putExtra("event_audience", event.getAudience());
            intent.putExtra("event_category", event.getCategory());
            intent.putExtra("event_organizer", event.getOrganizer());
            intent.putExtra("event_banner_url", event.getBannerImageUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventBanner;
        TextView eventTitle, eventAudience, eventDate, eventLocation;
        TextView dateBadge, categoryTag;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventBanner = itemView.findViewById(R.id.event_banner);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventAudience = itemView.findViewById(R.id.event_audience);
            eventDate = itemView.findViewById(R.id.event_date);
            eventLocation = itemView.findViewById(R.id.event_location);
            dateBadge = itemView.findViewById(R.id.date_badge);
            categoryTag = itemView.findViewById(R.id.category_tag);
        }
    }
}

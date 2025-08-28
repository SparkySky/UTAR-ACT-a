package com.meow.utaract.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meow.utaract.R;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> eventList;

    public EventsAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.eventTitle.setText(event.getTitle());
        holder.eventDate.setText(event.getDate());
        holder.eventLocation.setText(event.getLocation());
        holder.eventAudience.setText(event.getAudience());
        holder.categoryTag.setText(event.getCategory());
        holder.dateBadge.setText(event.getDateBadge());
        holder.eventBanner.setImageResource(event.getBannerResId());
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventBanner;
        TextView dateBadge, categoryTag, eventTitle, eventAudience, eventDate, eventLocation;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventBanner = itemView.findViewById(R.id.event_banner);
            dateBadge = itemView.findViewById(R.id.date_badge);
            categoryTag = itemView.findViewById(R.id.category_tag);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventAudience = itemView.findViewById(R.id.event_audience);
            eventDate = itemView.findViewById(R.id.event_date);
            eventLocation = itemView.findViewById(R.id.event_location);
        }
    }
}

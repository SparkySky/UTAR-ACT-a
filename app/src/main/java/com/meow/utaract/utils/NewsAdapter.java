package com.meow.utaract.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.meow.utaract.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    public interface NewsInteractionListener {
        void onLikeNews(News news);
        void onUnlikeNews(News news);
    }

    private List<News> newsList;
    private Context context;
    private NewsInteractionListener listener;
    private String currentUserId;

    public NewsAdapter(List<News> newsList, Context context, NewsInteractionListener listener) {
        this.newsList = newsList;
        this.context = context;
        this.listener = listener;
        this.currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void updateNews(List<News> newNews) {
        this.newsList = newNews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News news = newsList.get(position);
        holder.bind(news);
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView organizerProfileImage;
        TextView organizerName, postedDate, newsContent, likeCount;
        ImageButton likeButton;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            organizerProfileImage = itemView.findViewById(R.id.organizerProfileImage);
            organizerName = itemView.findViewById(R.id.organizerName);
            postedDate = itemView.findViewById(R.id.postedDate);
            newsContent = itemView.findViewById(R.id.newsContent);
            likeCount = itemView.findViewById(R.id.likeCount);
            likeButton = itemView.findViewById(R.id.likeButton);
        }

        void bind(News news) {
            organizerName.setText(news.getOrganizerName());
            newsContent.setText(news.getContent());
            likeCount.setText(String.valueOf(news.getLikeCount()));

            // Format date
            String timeAgo = getTimeAgo(news.getPostedDate());
            postedDate.setText(timeAgo);

            // Load profile image
            if (news.getOrganizerProfilePic() != null && !news.getOrganizerProfilePic().isEmpty()) {
                Glide.with(context)
                        .load(news.getOrganizerProfilePic())
                        .placeholder(R.drawable.icon_bar_avatar)
                        .into(organizerProfileImage);
            }

            // Set like button state
            boolean isLiked = news.getLikedBy() != null && news.getLikedBy().contains(currentUserId);
            likeButton.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            if (isLiked) {
                likeButton.setColorFilter(context.getResources().getColor(R.color.md_theme_light_primary));
            } else {
                likeButton.setColorFilter(context.getResources().getColor(android.R.color.darker_gray));
            }

            // Set up like button click listener
            likeButton.setOnClickListener(v -> {
                if (isLiked) {
                    listener.onUnlikeNews(news);
                } else {
                    listener.onLikeNews(news);
                }
            });
        }

        private String getTimeAgo(Date date) {
            if (date == null) return "Just now";

            long now = System.currentTimeMillis();
            long diff = now - date.getTime();

            if (diff < 60000) return "Just now";
            if (diff < 3600000) return (diff / 60000) + " minutes ago";
            if (diff < 86400000) return (diff / 3600000) + " hours ago";
            if (diff < 604800000) return (diff / 86400000) + " days ago";

            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date);
        }
    }
}
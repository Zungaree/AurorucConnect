package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.List;

public class RssItemAdapter extends RecyclerView.Adapter<RssItemAdapter.RssItemViewHolder> {
    private List<RssItem> rssItems;

    public RssItemAdapter(List<RssItem> rssItems) {
        this.rssItems = rssItems;
    }

    @NonNull
    @Override
    public RssItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rss_feed, parent, false);
        return new RssItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RssItemViewHolder holder, int position) {
        RssItem item = rssItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return rssItems.size();
    }

    public void updateItems(List<RssItem> newItems) {
        this.rssItems = newItems;
        notifyDataSetChanged();
    }

    static class RssItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView titleTextView;
        private TextView descriptionTextView;

        public RssItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.rssItemImage);
            titleTextView = itemView.findViewById(R.id.rssItemTitle);
            descriptionTextView = itemView.findViewById(R.id.rssItemDescription);
        }

        public void bind(RssItem item) {
            titleTextView.setText(item.getTitle());
            descriptionTextView.setText(item.getDescription());

            // Load image with Glide
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                imageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.default_profile_picture)
                        .error(R.drawable.default_profile_picture)
                        .into(imageView);
            } else {
                imageView.setVisibility(View.GONE);
            }

            // Handle click to open link
            itemView.setOnClickListener(v -> {
                if (item.getLink() != null && !item.getLink().isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getLink()));
                    v.getContext().startActivity(intent);
                }
            });
        }
    }
} 
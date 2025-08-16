package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RssHorizontalAdapter extends RecyclerView.Adapter<RssHorizontalAdapter.RssViewHolder> {
    private final Context context;
    private final List<RssItem> items;

    public RssHorizontalAdapter(Context context, List<RssItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public RssViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_horizontal_card, parent, false);
        return new RssViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RssViewHolder holder, int position) {
        RssItem item = items.get(position);
        holder.title.setText(item.getTitle());
        // Description may include HTML; strip tags for the preview
        String desc = item.getDescription();
        if (!TextUtils.isEmpty(desc)) {
            holder.description.setText(Html.fromHtml(desc).toString().trim());
        } else {
            holder.description.setText("");
        }

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(item.getImageUrl())
                .centerCrop()
                .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.platinum);
        }

        holder.itemView.setOnClickListener(v -> {
            if (item.getLink() != null && !item.getLink().isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getLink()));
                context.startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RssViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView description;

        RssViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cardImage);
            title = itemView.findViewById(R.id.cardTitle);
            description = itemView.findViewById(R.id.cardDescription);
        }
    }
}



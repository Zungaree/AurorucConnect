package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HorizontalCardAdapter extends RecyclerView.Adapter<HorizontalCardAdapter.CardViewHolder> {
    public interface OnCardClickListener {
        void onCardClick(HorizontalCardItem item);
    }

    private final Context context;
    private final List<HorizontalCardItem> items;
    private final OnCardClickListener listener;

    public HorizontalCardAdapter(Context context, List<HorizontalCardItem> items, OnCardClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_horizontal_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        HorizontalCardItem item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.image.setImageResource(item.getImageResId());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cardImage);
            title = itemView.findViewById(R.id.cardTitle);
        }
    }
}



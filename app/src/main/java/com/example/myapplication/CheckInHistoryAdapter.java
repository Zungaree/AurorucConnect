package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CheckInHistoryAdapter extends RecyclerView.Adapter<CheckInHistoryAdapter.ViewHolder> {
    private List<CheckInHistory> historyList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_check_in_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheckInHistory history = historyList.get(position);
        
        holder.userIdText.setText(history.getUserId());
        holder.userNameText.setText(history.getUserName());
        holder.timestampText.setText(history.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistoryList(List<CheckInHistory> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userIdText;
        TextView userNameText;
        TextView timestampText;

        ViewHolder(View itemView) {
            super(itemView);
            userIdText = itemView.findViewById(R.id.userIdText);
            userNameText = itemView.findViewById(R.id.userNameText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
    }
} 
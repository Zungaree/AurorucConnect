package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StoreCreditsHistoryAdapter extends RecyclerView.Adapter<StoreCreditsHistoryAdapter.ViewHolder> {
    private List<TransactionHistory> historyList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_store_credits_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionHistory history = historyList.get(position);
        
        // Format and set credits received
        holder.creditsReceivedText.setText("₱" + history.getCreditsReceived());
        
        // Format and set new total
        holder.newTotalCreditsText.setText("New Balance: ₱" + history.getNewTotalCredits());
        
        // Set timestamp
        holder.timestampText.setText(history.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistoryList(List<TransactionHistory> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView creditsReceivedText;
        TextView newTotalCreditsText;
        TextView timestampText;

        ViewHolder(View itemView) {
            super(itemView);
            creditsReceivedText = itemView.findViewById(R.id.creditsReceivedText);
            newTotalCreditsText = itemView.findViewById(R.id.newTotalCreditsText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
    }
} 
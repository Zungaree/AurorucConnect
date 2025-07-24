package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoreCreditsHistoryActivity extends AppCompatActivity {
    private static final String TAG = "StoreCreditsHistory";
    
    private RecyclerView historyRecyclerView;
    private Button backButton;
    private StoreCreditsHistoryAdapter adapter;
    private DatabaseReference historyRef;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_credits_history);

        // Initialize views
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        backButton = findViewById(R.id.backButton);

        // Set up RecyclerView
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StoreCreditsHistoryAdapter();
        historyRecyclerView.setAdapter(adapter);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Get user name and load history
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Getting user data for UID: " + currentUser.getUid());
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("name")) {
                        userName = snapshot.child("name").getValue(String.class);
                        Log.d(TAG, "Got username: " + userName);
                        if (userName != null) {
                            loadTransactionHistory();
                        } else {
                            Log.e(TAG, "Username is null");
                            Toast.makeText(StoreCreditsHistoryActivity.this,
                                "Error: Could not get username",
                                Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "User data snapshot doesn't exist or doesn't have name");
                        Toast.makeText(StoreCreditsHistoryActivity.this,
                            "Error: User data not found",
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading user data: " + error.getMessage());
                    Toast.makeText(StoreCreditsHistoryActivity.this,
                        "Error loading user data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "No current user found");
            Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadTransactionHistory() {
        Log.d(TAG, "Loading transaction history for user: " + userName);
        historyRef = FirebaseDatabase.getInstance()
            .getReference("TBL_STORE_CREDITS_HISTORY")
            .child(userName);

        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Got history data snapshot, exists: " + snapshot.exists());
                List<TransactionHistory> historyList = new ArrayList<>();
                
                if (snapshot.exists()) {
                    for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                        Log.d(TAG, "Processing transaction: " + transactionSnapshot.getKey());
                        try {
                            String creditsReceived = transactionSnapshot.child("creditsReceived").getValue(String.class);
                            Long newTotalCredits = transactionSnapshot.child("newTotalCredits").getValue(Long.class);
                            String timestamp = transactionSnapshot.child("timestamp").getValue(String.class);
                            String userId = transactionSnapshot.child("userId").getValue(String.class);

                            Log.d(TAG, String.format("Transaction data: credits=%s, total=%d, time=%s, user=%s",
                                creditsReceived, newTotalCredits, timestamp, userId));

                            if (creditsReceived != null && newTotalCredits != null && timestamp != null) {
                                TransactionHistory history = new TransactionHistory(
                                    creditsReceived,
                                    newTotalCredits,
                                    timestamp
                                );
                                historyList.add(history);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing transaction: " + e.getMessage());
                        }
                    }
                    
                    // Sort by timestamp (newest first)
                    Collections.reverse(historyList);
                    
                    Log.d(TAG, "Found " + historyList.size() + " transactions");
                    adapter.setHistoryList(historyList);
                    
                    if (historyList.isEmpty()) {
                        Toast.makeText(StoreCreditsHistoryActivity.this,
                            "No transaction history found",
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "No history found for user");
                    Toast.makeText(StoreCreditsHistoryActivity.this,
                        "No transaction history found",
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading history: " + error.getMessage());
                Toast.makeText(StoreCreditsHistoryActivity.this,
                    "Error loading history: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
} 
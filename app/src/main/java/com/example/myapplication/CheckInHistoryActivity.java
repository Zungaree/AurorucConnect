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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckInHistoryActivity extends AppCompatActivity {
    private static final String TAG = "CheckInHistory";
    
    private RecyclerView historyRecyclerView;
    private Button backButton;
    private CheckInHistoryAdapter adapter;
    private DatabaseReference historyRef;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_history);

        // Initialize views
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        backButton = findViewById(R.id.backButton);

        // Set up RecyclerView
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheckInHistoryAdapter();
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
                            loadCheckInHistory();
                        } else {
                            Log.e(TAG, "Username is null");
                            Toast.makeText(CheckInHistoryActivity.this,
                                "Error: Could not get username",
                                Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "User data snapshot doesn't exist or doesn't have name");
                        Toast.makeText(CheckInHistoryActivity.this,
                            "Error: User data not found",
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading user data: " + error.getMessage());
                    Toast.makeText(CheckInHistoryActivity.this,
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

    private void loadCheckInHistory() {
        Log.d(TAG, "Loading check-in history for user: " + userName);
        historyRef = FirebaseDatabase.getInstance()
            .getReference("TBL_USER_CHECKIN");

        // Query check-ins for current user
        historyRef.orderByChild("userId").equalTo(userName)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "Got check-in data snapshot, exists: " + snapshot.exists());
                    List<CheckInHistory> historyList = new ArrayList<>();
                    
                    if (snapshot.exists()) {
                        for (DataSnapshot checkInSnapshot : snapshot.getChildren()) {
                            Log.d(TAG, "Processing check-in: " + checkInSnapshot.getKey());
                            try {
                                String timestamp = checkInSnapshot.child("timestamp").getValue(String.class);
                                String userId = checkInSnapshot.child("userId").getValue(String.class);
                                String userName = checkInSnapshot.child("userName").getValue(String.class);

                                Log.d(TAG, String.format("Check-in data: time=%s, userId=%s, userName=%s",
                                    timestamp, userId, userName));

                                if (timestamp != null && userId != null && userName != null) {
                                    CheckInHistory history = new CheckInHistory(
                                        timestamp,
                                        userId,
                                        userName
                                    );
                                    historyList.add(history);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing check-in: " + e.getMessage());
                            }
                        }
                        
                        // Sort by timestamp (newest first)
                        Collections.reverse(historyList);
                        
                        Log.d(TAG, "Found " + historyList.size() + " check-ins");
                        adapter.setHistoryList(historyList);
                        
                        if (historyList.isEmpty()) {
                            Toast.makeText(CheckInHistoryActivity.this,
                                "No check-in history found",
                                Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "No check-in history found for user");
                        Toast.makeText(CheckInHistoryActivity.this,
                            "No check-in history found",
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading check-in history: " + error.getMessage());
                    Toast.makeText(CheckInHistoryActivity.this,
                        "Error loading check-in history: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
} 
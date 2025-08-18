package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
    
    private TextView currentDaysText;
    private TextView progressDescription;
    private ProgressBar progressBar;
    private Button historyButton;
    private Button backButton;
    
    // Milestone views
    private CardView milestone10Card, milestone20Card, milestone30Card, milestone40Card, milestone50Card;
    private TextView milestone10Text, milestone20Text, milestone30Text, milestone40Text, milestone50Text;
    private TextView milestone10Status, milestone20Status, milestone30Status, milestone40Status, milestone50Status;
    private ProgressBar milestone10Progress, milestone20Progress, milestone30Progress, milestone40Progress, milestone50Progress;
    
    private DatabaseReference historyRef;
    private String userName;
    private int totalDays = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_history);

        // Initialize views
        currentDaysText = findViewById(R.id.currentDaysText);
        progressDescription = findViewById(R.id.progressDescription);
        progressBar = findViewById(R.id.progressBar);
        historyButton = findViewById(R.id.historyButton);
        backButton = findViewById(R.id.backButton);

        // Initialize milestone views
        initializeMilestoneViews();


        // Get user name and load attendance data
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
                            loadAttendanceData();
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

    private void initializeMilestoneViews() {
        // Initialize milestone cards
        milestone10Card = findViewById(R.id.milestone10Card);
        milestone20Card = findViewById(R.id.milestone20Card);
        milestone30Card = findViewById(R.id.milestone30Card);
        milestone40Card = findViewById(R.id.milestone40Card);
        milestone50Card = findViewById(R.id.milestone50Card);

        // Initialize milestone text views
        milestone10Text = findViewById(R.id.milestone10Text);
        milestone20Text = findViewById(R.id.milestone20Text);
        milestone30Text = findViewById(R.id.milestone30Text);
        milestone40Text = findViewById(R.id.milestone40Text);
        milestone50Text = findViewById(R.id.milestone50Text);

        // Initialize milestone status views
        milestone10Status = findViewById(R.id.milestone10Status);
        milestone20Status = findViewById(R.id.milestone20Status);
        milestone30Status = findViewById(R.id.milestone30Status);
        milestone40Status = findViewById(R.id.milestone40Status);
        milestone50Status = findViewById(R.id.milestone50Status);

        // Initialize milestone progress bars
        milestone10Progress = findViewById(R.id.milestone10Progress);
        milestone20Progress = findViewById(R.id.milestone20Progress);
        milestone30Progress = findViewById(R.id.milestone30Progress);
        milestone40Progress = findViewById(R.id.milestone40Progress);
        milestone50Progress = findViewById(R.id.milestone50Progress);
    }

    private void loadAttendanceData() {
        Log.d(TAG, "Loading attendance data for user: " + userName);
        historyRef = FirebaseDatabase.getInstance()
            .getReference("TBL_USER_CHECKIN");

        // Query check-ins for current user
        historyRef.orderByChild("userId").equalTo(userName)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "Got check-in data snapshot, exists: " + snapshot.exists());
                    List<String> checkInDates = new ArrayList<>();
                    
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
                                    // Extract date from timestamp (assuming format like "2024-01-15 10:30:00")
                                    String date = timestamp.split(" ")[0];
                                    if (!checkInDates.contains(date)) {
                                        checkInDates.add(date);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing check-in: " + e.getMessage());
                            }
                        }
                        
                        // Sort dates
                        Collections.sort(checkInDates);
                        
                        totalDays = checkInDates.size();
                        Log.d(TAG, "Found " + totalDays + " unique check-in days");
                        
                        updateBattlePassProgress();
                        updateMilestones();
                        
                    } else {
                        Log.d(TAG, "No check-in history found for user");
                        totalDays = 0;
                        updateBattlePassProgress();
                        updateMilestones();
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

    private void updateBattlePassProgress() {
        // Update current days text
        currentDaysText.setText(totalDays + " Days");
        
        // Calculate progress towards next milestone (50 days total)
        int nextMilestone = 50;
        int progress = (int) ((double) totalDays / nextMilestone * 100);
        progressBar.setProgress(progress);
        
        // Update progress description
        int daysRemaining = nextMilestone - totalDays;
        if (daysRemaining > 0) {
            progressDescription.setText(daysRemaining + " more days for a free silver loyalty card!");
        } else {
            progressDescription.setText("Congratulations! You've reached the maximum milestone!");
        }
        
        Log.d(TAG, "Updated Battle Pass progress: " + totalDays + " days, " + progress + "% progress");
    }

    private void updateMilestones() {
        // Update each milestone based on user's progress
        updateMilestone(10, milestone10Card, milestone10Text, milestone10Status, milestone10Progress);
        updateMilestone(20, milestone20Card, milestone20Text, milestone20Status, milestone20Progress);
        updateMilestone(30, milestone30Card, milestone30Text, milestone30Status, milestone30Progress);
        updateMilestone(40, milestone40Card, milestone40Text, milestone40Status, milestone40Progress);
        updateMilestone(50, milestone50Card, milestone50Text, milestone50Status, milestone50Progress);
    }

    private void updateMilestone(int milestoneDays, CardView card, TextView textView, TextView statusView, ProgressBar progressBar) {
        // Calculate progress percentage for this milestone
        int progressPercentage = Math.min((totalDays * 100) / milestoneDays, 100);
        progressBar.setProgress(progressPercentage);
        
        if (totalDays >= milestoneDays) {
            // Milestone completed
            card.setCardBackgroundColor(getResources().getColor(R.color.app_turquoise));
            textView.setTextColor(getResources().getColor(android.R.color.white));
            statusView.setTextColor(getResources().getColor(android.R.color.white));
            statusView.setText("✓ Claimed");
            textView.setText(milestoneDays + " Days Complete!");
            progressBar.setProgress(100);
        } else {
            // Milestone not yet reached
            card.setCardBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            textView.setTextColor(getResources().getColor(R.color.primary_dark));
            statusView.setTextColor(getResources().getColor(R.color.primary_dark));
            statusView.setText(totalDays + "/" + milestoneDays);
            textView.setText(milestoneDays + " Days");
        }
    }


}
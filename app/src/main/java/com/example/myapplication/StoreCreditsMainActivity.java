package com.example.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class StoreCreditsMainActivity extends AppCompatActivity {
    private TextView creditAmountText;
    private Button useCreditsButton;
    private Button historyButton;
    private Button learnMoreButton;
    private Button backButton;
    
    private DatabaseReference userRef;
    private DatabaseReference creditsRef;
    private ValueEventListener creditsListener;
    private String userName;

    // Mode removed: unified flow

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_credits_main);

        // Initialize views
        creditAmountText = findViewById(R.id.creditAmount);
        useCreditsButton = findViewById(R.id.useCreditsButton);
        historyButton = findViewById(R.id.historyButton);
        learnMoreButton = findViewById(R.id.learnMoreButton);
        backButton = findViewById(R.id.backButton);

        // Set up Firebase references
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // First get the user's name from users node
            userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid());
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("name")) {
                        userName = snapshot.child("name").getValue(String.class);
                        if (userName != null) {
                            // Now that we have the username, query TBL_USER_TOTAL_CREDITS
                            loadUserCredits(userName);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(StoreCreditsMainActivity.this, 
                        "Error loading user data: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Set up unified action (no amount input, just start NFC screen)
        useCreditsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StoreCreditsCheckInActivity.class);
            startActivity(intent);
        });

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StoreCreditsHistoryActivity.class);
            startActivity(intent);
        });

        learnMoreButton.setOnClickListener(v -> {
            // TODO: Implement learn more
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void loadUserCredits(String userName) {
        creditsRef = FirebaseDatabase.getInstance()
            .getReference("TBL_USER_TOTAL_CREDITS");
            
        // Query to find the entry where userId matches the user's name
        Query query = creditsRef.orderByChild("userId").equalTo(userName);
        
        creditsListener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        // Get the totalCredits value
                        Long credits = userSnapshot.child("totalCredits").getValue(Long.class);
                        if (credits != null) {
                            creditAmountText.setText(String.valueOf(credits));
                        }
                        
                        // Get last updated time if needed
                        String lastUpdated = userSnapshot.child("lastUpdated").getValue(String.class);
                        // You can use lastUpdated if needed
                    }
                } else {
                    // No credits found for this user
                    creditAmountText.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StoreCreditsMainActivity.this, 
                    "Error loading credits: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Amount dialog removed per requirement (send name and email only)

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (creditsRef != null && creditsListener != null) {
            creditsRef.removeEventListener(creditsListener);
        }
    }
} 
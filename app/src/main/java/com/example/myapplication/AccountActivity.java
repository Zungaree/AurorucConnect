package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountActivity extends AppCompatActivity {
    private static final String TAG = "AccountActivity";
    
    private TextView userNameText;
    private TextView userEmailText;
    private TextView membershipText;
    private TextView accountCreatedText;
    private Button logoutButton;
    private Button backButton;
    
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize views
        userNameText = findViewById(R.id.userNameText);
        userEmailText = findViewById(R.id.userEmailText);
        membershipText = findViewById(R.id.membershipText);
        accountCreatedText = findViewById(R.id.accountCreatedText);
        logoutButton = findViewById(R.id.logoutButton);
        backButton = findViewById(R.id.backButton);

        // Set up button click listeners
        logoutButton.setOnClickListener(v -> handleLogout());
        backButton.setOnClickListener(v -> finish());

        // Load user data
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Set email from Firebase Auth
            userEmailText.setText(currentUser.getEmail());
            
            // Get user data from Firebase Database
            userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Get user name
                        String userName = snapshot.child("name").getValue(String.class);
                        if (userName != null && !userName.isEmpty()) {
                            userNameText.setText(userName);
                        } else {
                            userNameText.setText("Not set");
                        }

                        // Get account creation date
                        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
                        if (createdAt != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                            String dateString = sdf.format(new Date(createdAt));
                            accountCreatedText.setText(dateString);
                        } else {
                            accountCreatedText.setText("Unknown");
                        }

                        Log.d(TAG, "User data loaded successfully");
                    } else {
                        Log.e(TAG, "User data not found in database");
                        userNameText.setText("Not found");
                        accountCreatedText.setText("Unknown");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading user data: " + error.getMessage());
                    Toast.makeText(AccountActivity.this,
                        "Error loading user data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    userNameText.setText("Error loading data");
                    accountCreatedText.setText("Unknown");
                }
            });
        } else {
            Log.e(TAG, "No current user found");
            Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleLogout() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> {
                // Sign out from Firebase
                firebaseAuth.signOut();
                
                // Navigate to login screen
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}

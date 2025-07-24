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
    private Button receiveButton;
    private Button payButton;
    private Button historyButton;
    private Button learnMoreButton;
    private Button backButton;
    
    private DatabaseReference userRef;
    private DatabaseReference creditsRef;
    private ValueEventListener creditsListener;
    private String userName;

    private static final String MODE_RECEIVE = "receive";
    private static final String MODE_PAY = "pay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_credits_main);

        // Initialize views
        creditAmountText = findViewById(R.id.creditAmount);
        receiveButton = findViewById(R.id.receiveButton);
        payButton = findViewById(R.id.payButton);
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

        // Set up button clicks
        receiveButton.setOnClickListener(v -> {
            showAmountInputDialog(MODE_RECEIVE);
        });

        payButton.setOnClickListener(v -> {
            showAmountInputDialog(MODE_PAY);
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

    private void showAmountInputDialog(String mode) {
        // Create and show the dialog for amount input
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_amount_input);
        dialog.getWindow().setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );

        TextView titleTextView = dialog.findViewById(R.id.dialogTitleTextView);
        EditText amountEditText = dialog.findViewById(R.id.amountEditText);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button confirmButton = dialog.findViewById(R.id.confirmButton);

        // Set title based on mode
        titleTextView.setText(mode.equals(MODE_RECEIVE) ? "Enter Amount to Receive" : "Enter Amount to Pay");

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(v -> {
            String amount = amountEditText.getText().toString();
            if (!amount.isEmpty()) {
                try {
                    double amountValue = Double.parseDouble(amount);
                    
                    // For pay mode, check if user has enough credits
                    if (mode.equals(MODE_PAY)) {
                        String currentCreditsText = creditAmountText.getText().toString();
                        double currentCredits = Double.parseDouble(currentCreditsText);
                        if (amountValue > currentCredits) {
                            amountEditText.setError("Insufficient credits");
                            return;
                        }
                    }
                    
                    // Launch store credits check-in activity
                    Intent intent = new Intent(this, StoreCreditsCheckInActivity.class);
                    intent.putExtra("amount", amount);
                    intent.putExtra("mode", mode);
                    startActivity(intent);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    amountEditText.setError("Invalid amount");
                }
            } else {
                amountEditText.setError("Please enter an amount");
            }
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (creditsRef != null && creditsListener != null) {
            creditsRef.removeEventListener(creditsListener);
        }
    }
} 
package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class StoreCreditsActivity extends AppCompatActivity {
    private TextView balanceTextView;
    private Button topUpButton;
    private Button payButton;
    private DatabaseReference userCreditsRef;
    private double currentBalance = 0.0;
    private static final String CREDITS_TYPE_TOPUP = "topup";
    private static final String CREDITS_TYPE_PAY = "pay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_credits);

        // Initialize views
        balanceTextView = findViewById(R.id.balanceTextView);
        topUpButton = findViewById(R.id.topUpButton);
        payButton = findViewById(R.id.payButton);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        userCreditsRef = database.getReference("users").child("credits");

        // Set up button click listeners
        topUpButton.setOnClickListener(v -> showAmountDialog(CREDITS_TYPE_TOPUP));
        payButton.setOnClickListener(v -> showAmountDialog(CREDITS_TYPE_PAY));

        // Load current balance
        loadBalance();
    }

    private void loadBalance() {
        userCreditsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentBalance = snapshot.getValue(Double.class);
                }
                updateBalanceDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StoreCreditsActivity.this, 
                    "Error loading balance: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBalanceDisplay() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        balanceTextView.setText(format.format(currentBalance));
    }

    private void showAmountDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_amount_input, null);
        
        TextView titleTextView = dialogView.findViewById(R.id.dialogTitleTextView);
        TextInputEditText amountEditText = dialogView.findViewById(R.id.amountEditText);

        titleTextView.setText(type.equals(CREDITS_TYPE_TOPUP) ? "Top Up Amount" : "Payment Amount");

        builder.setView(dialogView)
               .setPositiveButton("Confirm", null)
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent dialog from dismissing on invalid input
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String amountStr = amountEditText.getText().toString();
            if (amountStr.isEmpty()) {
                amountEditText.setError("Please enter an amount");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    amountEditText.setError("Amount must be greater than 0");
                    return;
                }
                if (type.equals(CREDITS_TYPE_PAY) && amount > currentBalance) {
                    amountEditText.setError("Insufficient balance");
                    return;
                }
            } catch (NumberFormatException e) {
                amountEditText.setError("Invalid amount");
                return;
            }

            // Start NFC activity
            Intent intent = new Intent(this, CreditNfcActivity.class);
            intent.putExtra("amount", amount);
            intent.putExtra("type", type);
            startActivity(intent);
            dialog.dismiss();
        });
    }
} 
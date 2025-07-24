package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class CreditNfcActivity extends AppCompatActivity {
    private static final String TAG = "CreditNfcActivity";
    
    private TextView amountTextView;
    private TextView statusTextView;
    private ImageView nfcImageView;
    private NfcAdapter nfcAdapter;
    private double amount;
    private String type;
    
    // BroadcastReceiver to handle NFC status updates and logs from our service
    private BroadcastReceiver nfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyHostApduService.ACTION_NFC_STATUS.equals(action)) {
                boolean connected = intent.getBooleanExtra(MyHostApduService.EXTRA_NFC_CONNECTED, false);
                updateNfcStatus(connected);
            } else if (MyHostApduService.ACTION_NFC_LOG.equals(action)) {
                String logMsg = intent.getStringExtra(MyHostApduService.EXTRA_LOG_MESSAGE);
                if (logMsg != null) {
                    Toast.makeText(CreditNfcActivity.this, logMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_nfc);

        // Get amount and type from intent
        amount = getIntent().getDoubleExtra("amount", 0.0);
        type = getIntent().getStringExtra("type");

        // Initialize views
        amountTextView = findViewById(R.id.amountTextView);
        statusTextView = findViewById(R.id.statusTextView);
        nfcImageView = findViewById(R.id.nfcImageView);

        // Format and display amount
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        String formattedAmount = format.format(amount);
        amountTextView.setText(formattedAmount);

        // Update title based on transaction type
        TextView titleTextView = findViewById(R.id.titleTextView);
        titleTextView.setText(type.equals("topup") ? "Top Up Credits" : "Pay with Credits");

        // Initialize NFC
        initNfc();
        
        // Start HCE service with amount data
        startHceService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for broadcasts from our service
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyHostApduService.ACTION_NFC_STATUS);
        filter.addAction(MyHostApduService.ACTION_NFC_LOG);
        registerReceiver(nfcReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        
        // Check NFC status
        checkNfcStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(nfcReceiver);
        stopHceService();
    }
    
    private void initNfc() {
        NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager.getDefaultAdapter();
        
        if (nfcAdapter == null) {
            // Device does not support NFC
            statusTextView.setText(R.string.nfc_not_available);
            nfcImageView.setImageResource(android.R.drawable.ic_dialog_alert);
            Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_LONG).show();
            finish();
        } else {
            checkNfcStatus();
        }
    }
    
    private void checkNfcStatus() {
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            statusTextView.setText(R.string.nfc_disabled);
            nfcImageView.setImageResource(android.R.drawable.ic_dialog_alert);
            Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void startHceService() {
        Intent intent = new Intent(this, MyHostApduService.class);
        intent.putExtra("amount", amount);
        intent.putExtra("type", type);
        startService(intent);
    }
    
    private void stopHceService() {
        stopService(new Intent(this, MyHostApduService.class));
    }
    
    private void updateNfcStatus(boolean connected) {
        if (connected) {
            statusTextView.setText("NFC Reader Connected - Processing...");
            nfcImageView.setImageResource(android.R.drawable.ic_dialog_info);
            Toast.makeText(this, "NFC Reader detected!", Toast.LENGTH_SHORT).show();
        } else {
            statusTextView.setText("Waiting for NFC reader...");
            nfcImageView.setImageResource(android.R.drawable.ic_lock_lock);
        }
    }
} 
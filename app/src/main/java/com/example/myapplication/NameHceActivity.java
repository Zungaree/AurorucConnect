package com.example.myapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class NameHceActivity extends AppCompatActivity {
    private static final String TAG = "NameHceActivity";
    
    private NfcAdapter nfcAdapter;
    private TextView statusTextView;
    private TextInputEditText nameEditText;
    private Button startNameHceButton;
    private boolean isHceRunning = false;
    
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
                    Toast.makeText(NameHceActivity.this, logMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_hce);
        
        // Initialize views
        statusTextView = findViewById(R.id.statusTextView);
        nameEditText = findViewById(R.id.nameEditText);
        startNameHceButton = findViewById(R.id.startNameHceButton);
        
        // Initialize NFC
        initNfc();
        
        // Set up button click listener
        startNameHceButton.setOnClickListener(v -> toggleHce());
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
    }
    
    private void initNfc() {
        NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager.getDefaultAdapter();
        
        if (nfcAdapter == null) {
            // Device does not support NFC
            statusTextView.setText(R.string.nfc_not_available);
            startNameHceButton.setEnabled(false);
            Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_LONG).show();
        } else {
            checkNfcStatus();
        }
    }
    
    private void checkNfcStatus() {
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            statusTextView.setText(R.string.nfc_disabled);
            startNameHceButton.setEnabled(false);
            Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show();
        }
    }
    
    private void toggleHce() {
        if (!isHceRunning) {
            String name = nameEditText.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Start HCE service with the name
            Intent intent = new Intent(this, MyHostApduService.class);
            intent.putExtra("name", name);
            startService(intent);
            
            startNameHceButton.setText("Stop Sending Name");
            statusTextView.setText("Ready to send: " + name);
            isHceRunning = true;
        } else {
            // Stop HCE service
            stopService(new Intent(this, MyHostApduService.class));
            
            startNameHceButton.setText("Start Sending Name");
            statusTextView.setText("Ready to send");
            isHceRunning = false;
        }
    }
    
    private void updateNfcStatus(boolean connected) {
        if (connected) {
            statusTextView.setText("NFC Reader Connected - Sending Name...");
            Toast.makeText(this, "NFC Reader detected!", Toast.LENGTH_SHORT).show();
        } else {
            statusTextView.setText(isHceRunning ? "Ready to send: " + nameEditText.getText().toString() : "Ready to send");
        }
    }
} 
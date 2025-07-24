package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class StoreCreditsCheckInActivity extends AppCompatActivity {
    private static final String TAG = "StoreCreditsCheckIn";
    
    private NfcAdapter nfcAdapter;
    private TextView amountTextView;
    private TextView instructionsTextView;
    private ComponentName hceService;
    private String amount;
    private String mode;
    
    private BroadcastReceiver nfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (StoreCreditsHceService.ACTION_NFC_STATUS.equals(action)) {
                boolean connected = intent.getBooleanExtra(StoreCreditsHceService.EXTRA_NFC_CONNECTED, false);
                if (connected) {
                    Toast.makeText(StoreCreditsCheckInActivity.this, 
                        mode.equals("receive") ? "Credits received!" : "Payment successful!", 
                        Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_credits_checkin);
        
        // Get amount and mode from intent
        amount = getIntent().getStringExtra("amount");
        mode = getIntent().getStringExtra("mode");
        
        // Initialize views
        amountTextView = findViewById(R.id.amountTextView);
        instructionsTextView = findViewById(R.id.instructionsTextView);
        
        // Set text based on mode
        amountTextView.setText("₱" + amount);
        instructionsTextView.setText(mode.equals("receive") ? 
            "Hold your phone near the terminal to receive credits" :
            "Hold your phone near the terminal to pay");
        
        // Initialize HCE service component for store credits
        hceService = new ComponentName(this, StoreCreditsHceService.class);
        
        // Initialize NFC
        initNfc();
        
        // Start HCE service with amount and mode
        startHceService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for broadcasts from our service
        IntentFilter filter = new IntentFilter();
        filter.addAction(StoreCreditsHceService.ACTION_NFC_STATUS);
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
            Toast.makeText(this, "This device doesn't support NFC", Toast.LENGTH_LONG).show();
            finish();
        } else if (!nfcAdapter.isEnabled()) {
            showNfcSettings();
        }
    }
    
    private void startHceService() {
        // Enable HCE service
        getPackageManager().setComponentEnabledSetting(
            hceService,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        );
        
        // Start service with amount and mode
        Intent serviceIntent = new Intent(this, StoreCreditsHceService.class);
        serviceIntent.putExtra("amount", amount);
        serviceIntent.putExtra("mode", mode);
        startService(serviceIntent);
    }
    
    private void stopHceService() {
        getPackageManager().setComponentEnabledSetting(
            hceService,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
        stopService(new Intent(this, StoreCreditsHceService.class));
    }
    
    private void showNfcSettings() {
        Toast.makeText(this, "Please enable NFC", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
    }
    
    private void checkNfcStatus() {
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            showNfcSettings();
        }
    }
} 
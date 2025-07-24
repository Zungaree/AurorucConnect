package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CheckInActivity extends AppCompatActivity {
    private Button cancelButton;
    
    // BroadcastReceiver to handle NFC status updates and logs from our service
    private BroadcastReceiver nfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyHostApduService.ACTION_NFC_STATUS.equals(action)) {
                boolean connected = intent.getBooleanExtra(MyHostApduService.EXTRA_NFC_CONNECTED, false);
                if (connected) {
                    // NFC reader connected, show success message and finish
                    Toast.makeText(CheckInActivity.this, "Check-in successful!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            // Stop HCE service and finish activity
            stopHceService();
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for broadcasts from our service
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyHostApduService.ACTION_NFC_STATUS);
        registerReceiver(nfcReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        unregisterReceiver(nfcReceiver);
    }

    private void stopHceService() {
        getPackageManager().setComponentEnabledSetting(
            new ComponentName(this, MyHostApduService.class),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }
} 
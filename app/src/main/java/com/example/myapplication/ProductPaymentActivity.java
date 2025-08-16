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

public class ProductPaymentActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private TextView instructionsTextView;
    private ComponentName hceService;

    private final BroadcastReceiver nfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ProductPaymentHceService.ACTION_NFC_STATUS.equals(action)) {
                boolean connected = intent.getBooleanExtra(ProductPaymentHceService.EXTRA_NFC_CONNECTED, false);
                if (connected) {
                    // connected
                }
            } else if (ProductPaymentHceService.ACTION_NFC_RESULT.equals(action)) {
                boolean success = intent.getBooleanExtra(ProductPaymentHceService.EXTRA_NFC_SUCCESS, false);
                String message = intent.getStringExtra(ProductPaymentHceService.EXTRA_NFC_RESULT_MESSAGE);
                Toast.makeText(ProductPaymentActivity.this,
                        message != null ? message : (success ? "Success" : "Failed"),
                        Toast.LENGTH_SHORT).show();
                if (success) finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_payment);

        instructionsTextView = findViewById(R.id.instructionsTextView);
        String productName = getIntent().getStringExtra("productName");
        instructionsTextView.setText("Hold near terminal to pay for: " + (productName != null ? productName : "Item"));

        hceService = new ComponentName(this, ProductPaymentHceService.class);
        initNfc();
        startHceService(productName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ProductPaymentHceService.ACTION_NFC_STATUS);
        filter.addAction(ProductPaymentHceService.ACTION_NFC_RESULT);
        registerReceiver(nfcReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
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

    private void startHceService(String productName) {
        getPackageManager().setComponentEnabledSetting(
                hceService,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
        Intent i = new Intent(this, ProductPaymentHceService.class);
        i.putExtra("productName", productName);
        startService(i);
    }

    private void stopHceService() {
        getPackageManager().setComponentEnabledSetting(
                hceService,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
        stopService(new Intent(this, ProductPaymentHceService.class));
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



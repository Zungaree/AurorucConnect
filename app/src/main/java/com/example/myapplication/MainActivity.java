package com.example.myapplication;

import android.app.Dialog;
import android.app.PendingIntent;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private NfcAdapter nfcAdapter;
    private TextView welcomeTextView;
    private ImageView cardImageView;
    private Button checkInButton;
    private Button battlePassButton;
    private Button storeCreditsButton;
    private Button accountButton;
    private Button logoutButton;
    private ComponentName hceService;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;
    
    // BroadcastReceiver to handle NFC status updates and logs from our service
    private BroadcastReceiver nfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyHostApduService.ACTION_NFC_STATUS.equals(action)) {
                boolean connected = intent.getBooleanExtra(MyHostApduService.EXTRA_NFC_CONNECTED, false);
                if (connected) {
                    Toast.makeText(MainActivity.this, "NFC reader connected!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        // Initialize views
        welcomeTextView = findViewById(R.id.welcomeTextView);
        cardImageView = findViewById(R.id.cardImageView);
        checkInButton = findViewById(R.id.checkInButton);
        battlePassButton = findViewById(R.id.battlePassButton);
        storeCreditsButton = findViewById(R.id.storeCreditsButton);
        accountButton = findViewById(R.id.accountButton);
        logoutButton = findViewById(R.id.logoutButton);
        
        // Initialize HCE service component
        hceService = new ComponentName(this, MyHostApduService.class);
        
        // Set up button click listeners
        checkInButton.setOnClickListener(v -> startCheckIn());
        battlePassButton.setOnClickListener(v -> openBattlePass());
        storeCreditsButton.setOnClickListener(v -> openStoreCredits());
        accountButton.setOnClickListener(v -> openAccount());
        logoutButton.setOnClickListener(v -> handleLogout());
        
        // Initialize NFC
        initNfc();
        
        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Update user info
        updateUserInfo();
        
        // Ensure HCE service is stopped initially
        stopHceService();
    }

    private void updateUserInfo() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // Fetch user data from Firebase Database
            usersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String userName = dataSnapshot.child("name").getValue(String.class);
                        if (userName != null && !userName.isEmpty()) {
                            welcomeTextView.setText("Welcome, " + userName);
                        } else {
                            welcomeTextView.setText("Welcome, User");
                        }
                    } else {
                        welcomeTextView.setText("Welcome, User");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    welcomeTextView.setText("Welcome, User");
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for broadcasts from our service
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyHostApduService.ACTION_NFC_STATUS);
        registerReceiver(nfcReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        
        // Check NFC status when returning to the app
        checkNfcStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        unregisterReceiver(nfcReceiver);
    }
    
    private void startCheckIn() {
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            // Start HCE service
            getPackageManager().setComponentEnabledSetting(
                hceService,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            );
            
            // Launch check-in activity
            Intent intent = new Intent(this, CheckInActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "NFC must be enabled first", Toast.LENGTH_SHORT).show();
            showNfcSettings();
        }
    }
    
    private void stopHceService() {
        getPackageManager().setComponentEnabledSetting(
            hceService,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }
    
    private void initNfc() {
        // Get NFC adapter instance
        NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager.getDefaultAdapter();
        
        if (nfcAdapter == null) {
            // Device does not support NFC
            Toast.makeText(this, "This device doesn't support NFC", Toast.LENGTH_LONG).show();
        } else if (!nfcAdapter.isEnabled()) {
            // NFC is not enabled
            showNfcSettings();
        }
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

    private void openBattlePass() {
        Intent intent = new Intent(this, CheckInHistoryActivity.class);
        startActivity(intent);
                    }

    private void openStoreCredits() {
        Intent intent = new Intent(this, StoreCreditsMainActivity.class);
        startActivity(intent);
    }
    
    private void openAccount() {
        // TODO: Implement Account screen
        Toast.makeText(this, "Account settings coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void handleLogout() {
        firebaseAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
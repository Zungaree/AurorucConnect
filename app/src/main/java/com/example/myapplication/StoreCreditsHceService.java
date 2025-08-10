package com.example.myapplication;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class StoreCreditsHceService extends HostApduService {
    private static final String TAG = "StoreCreditsHceService";
    
    // ISO-DEP command HEADER for selecting an AID
    private static final String SELECT_APDU_HEADER = "00A40400";
    
    // Our custom AID we registered in store_credits_apdu_service.xml
    private static final String CUSTOM_AID = "F0010203040506";
    
    // "OK" status word sent in response (9000 hex)
    private static final byte[] STATUS_SUCCESS = {(byte) 0x90, (byte) 0x00};
    // "Command not allowed" status word sent in response (6986 hex)
    private static final byte[] STATUS_FAILED = {(byte) 0x69, (byte) 0x86};
    
    // NFC deactivation reasons
    public static final int DEACTIVATION_LINK_LOSS = 0;
    public static final int DEACTIVATION_DESELECTED = 1;
    
    // Broadcast action for NFC status updates
    public static final String ACTION_NFC_STATUS = "com.example.myapplication.NFC_STATUS";
    public static final String EXTRA_NFC_CONNECTED = "connected";
    
    // Broadcast action for log messages
    public static final String ACTION_NFC_LOG = "com.example.myapplication.NFC_LOG";
    public static final String EXTRA_LOG_MESSAGE = "log_message";
    
    // Broadcast action for transaction result
    public static final String ACTION_NFC_RESULT = "com.example.myapplication.NFC_RESULT";
    public static final String EXTRA_NFC_SUCCESS = "success";
    public static final String EXTRA_NFC_RESULT_MESSAGE = "message";

    // GET DATA command header
    private static final String GET_DATA_HEADER = "00CA0000";
    // Proprietary commands from reader to signal completion
    private static final String COMPLETE_SUCCESS_HEADER = "80500000"; // success ack
    private static final String COMPLETE_FAILURE_HEADER = "80510000"; // failure ack
    
    private String userName = "";
    private String userEmail = "";
    private DatabaseReference userRef;

    @Override
    public void onCreate() {
        super.onCreate();
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());
                
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        if (snapshot.hasChild("name")) {
                            userName = snapshot.child("name").getValue(String.class);
                        }
                        if (snapshot.hasChild("email")) {
                            userEmail = snapshot.child("email").getValue(String.class);
                        }
                        Log.d(TAG, "User loaded: name=" + userName + ", email=" + userEmail);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Failed to load user data: " + error.getMessage());
                }
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No amount handling; we only send name and email now
        return START_STICKY;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        String hexCommandApdu = ApduUtils.bytesToHex(commandApdu);
        Log.d(TAG, "Received APDU: " + hexCommandApdu);
        sendLogBroadcast("Received command: " + hexCommandApdu);
        
        // Handle GET DATA command for specific files
        if (hexCommandApdu.startsWith(GET_DATA_HEADER)) {
            if (commandApdu.length >= 6) {
                String fileNumber = String.format("%02X", commandApdu[5]);
                Log.d(TAG, "GET DATA for file: " + fileNumber);
                sendLogBroadcast("Reading file: " + fileNumber);
                
                String response;
                
                if ("01".equals(fileNumber)) {
                    // File 1: Username
                    response = (userName != null && !userName.isEmpty()) ? userName : "Unknown User";
                    Log.d(TAG, "Sending username: " + response);
                } else if ("02".equals(fileNumber)) {
                    // File 2: Email
                    response = (userEmail != null && !userEmail.isEmpty()) ? userEmail : "";
                    Log.d(TAG, "Sending email: " + response);
                } else {
                    return STATUS_FAILED;
                }
                
                sendLogBroadcast("File " + fileNumber + " content: " + response);
                return ApduUtils.buildResponse(response.getBytes(), ApduUtils.SW_SUCCESS);
            }
        }
        // Handle proprietary COMPLETE notifications from the reader
        else if (hexCommandApdu.startsWith(COMPLETE_SUCCESS_HEADER)) {
            Log.d(TAG, "Received COMPLETE SUCCESS from reader");
            sendResultBroadcast(true, "Transaction successful");
            return STATUS_SUCCESS;
        } else if (hexCommandApdu.startsWith(COMPLETE_FAILURE_HEADER)) {
            Log.d(TAG, "Received COMPLETE FAILURE from reader");
            sendResultBroadcast(false, "Transaction failed");
            return STATUS_SUCCESS;
        }
        // Handle SELECT AID command
        else if (ApduUtils.isSelectCommand(commandApdu)) {
            String aid = ApduUtils.extractAidFromSelect(commandApdu);
            Log.d(TAG, "AID: " + aid);
            sendLogBroadcast("SELECT AID: " + aid);
            
            if (aid.equals(CUSTOM_AID)) {
                sendNfcStatusBroadcast(true);
                sendLogBroadcast("AID matched - Connection established");
                return STATUS_SUCCESS;
            } else {
                Log.d(TAG, "Unknown AID: " + aid);
                sendLogBroadcast("Unknown AID received: " + aid);
                return STATUS_FAILED;
            }
        }
        
        Log.d(TAG, "Unsupported command: " + hexCommandApdu);
        sendLogBroadcast("Unsupported command received: " + hexCommandApdu);
        return ApduUtils.SW_INS_NOT_SUPPORTED;
    }

    @Override
    public void onDeactivated(int reason) {
        // The connection to the terminal was lost
        Log.d(TAG, "Deactivated: " + reason);
        String reasonText = (reason == DEACTIVATION_LINK_LOSS) ? "Link Lost" : "Deselected";
        sendLogBroadcast("Connection ended: " + reasonText);
        
        // Notify that we're not connected anymore
        sendNfcStatusBroadcast(false);
    }
    
    private void sendNfcStatusBroadcast(boolean isConnected) {
        // Send a broadcast to the activity to update UI
        Intent intent = new Intent(ACTION_NFC_STATUS);
        intent.putExtra(EXTRA_NFC_CONNECTED, isConnected);
        sendBroadcast(intent);
    }
    
    private void sendResultBroadcast(boolean success, String message) {
        Intent intent = new Intent(ACTION_NFC_RESULT);
        intent.putExtra(EXTRA_NFC_SUCCESS, success);
        intent.putExtra(EXTRA_NFC_RESULT_MESSAGE, message);
        sendBroadcast(intent);
    }

    private void sendLogBroadcast(String message) {
        // Send a broadcast to the activity to update the log
        Intent intent = new Intent(ACTION_NFC_LOG);
        intent.putExtra(EXTRA_LOG_MESSAGE, message);
        sendBroadcast(intent);
    }
} 
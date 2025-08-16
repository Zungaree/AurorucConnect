package com.example.myapplication;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProductPaymentHceService extends HostApduService {
    private static final String TAG = "ProductPaymentHCE";

    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final String GET_DATA_HEADER = "00CA0000";
    private static final String COMPLETE_SUCCESS_HEADER = "80500000";
    private static final String COMPLETE_FAILURE_HEADER = "80510000";

    // AID for this flow (reader expects this AID)
    private static final String CUSTOM_AID = "F12345678900";

    private static final byte[] STATUS_SUCCESS = {(byte) 0x90, (byte) 0x00};
    private static final byte[] STATUS_FAILED = {(byte) 0x69, (byte) 0x86};

    public static final String ACTION_NFC_STATUS = "com.example.myapplication.PROD_NFC_STATUS";
    public static final String EXTRA_NFC_CONNECTED = "connected";
    public static final String ACTION_NFC_RESULT = "com.example.myapplication.PROD_NFC_RESULT";
    public static final String EXTRA_NFC_SUCCESS = "success";
    public static final String EXTRA_NFC_RESULT_MESSAGE = "message";
    public static final String ACTION_NFC_LOG = "com.example.myapplication.PROD_NFC_LOG";
    public static final String EXTRA_LOG_MESSAGE = "log_message";

    private String userName = "";
    private String productName = "";

    @Override
    public void onCreate() {
        super.onCreate();
        loadUserName();
    }

    private void loadUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String n = snapshot.child("name").getValue(String.class);
                if (n != null) userName = n;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String pn = intent.getStringExtra("productName");
            if (pn != null) productName = pn;
        }
        return START_STICKY;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        String hex = ApduUtils.bytesToHex(commandApdu);
        Log.d(TAG, "APDU: " + hex);
        sendLog("CMD: " + hex);

        if (hex.startsWith(GET_DATA_HEADER)) {
            if (commandApdu.length >= 6) {
                String fileNum = String.format("%02X", commandApdu[5]);
                String resp;
                if ("01".equals(fileNum)) {
                    resp = (userName != null && !userName.isEmpty()) ? userName : "Unknown User";
                } else if ("02".equals(fileNum)) {
                    resp = (productName != null) ? productName : "";
                } else {
                    return STATUS_FAILED;
                }
                return ApduUtils.buildResponse(resp.getBytes(), ApduUtils.SW_SUCCESS);
            }
        } else if (hex.startsWith(COMPLETE_SUCCESS_HEADER)) {
            sendResult(true, "Payment successful");
            return STATUS_SUCCESS;
        } else if (hex.startsWith(COMPLETE_FAILURE_HEADER)) {
            sendResult(false, "Payment failed");
            return STATUS_SUCCESS;
        } else if (ApduUtils.isSelectCommand(commandApdu)) {
            String aid = ApduUtils.extractAidFromSelect(commandApdu);
            if (CUSTOM_AID.equals(aid)) {
                sendStatus(true);
                return STATUS_SUCCESS;
            } else {
                return STATUS_FAILED;
            }
        }

        return ApduUtils.SW_INS_NOT_SUPPORTED;
    }

    @Override
    public void onDeactivated(int reason) {
        sendStatus(false);
    }

    private void sendStatus(boolean connected) {
        Intent i = new Intent(ACTION_NFC_STATUS);
        i.putExtra(EXTRA_NFC_CONNECTED, connected);
        sendBroadcast(i);
    }

    private void sendResult(boolean success, String msg) {
        Intent i = new Intent(ACTION_NFC_RESULT);
        i.putExtra(EXTRA_NFC_SUCCESS, success);
        i.putExtra(EXTRA_NFC_RESULT_MESSAGE, msg);
        sendBroadcast(i);
    }

    private void sendLog(String message) {
        Intent i = new Intent(ACTION_NFC_LOG);
        i.putExtra(EXTRA_LOG_MESSAGE, message);
        sendBroadcast(i);
    }
}



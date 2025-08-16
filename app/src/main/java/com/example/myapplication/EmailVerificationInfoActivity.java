package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shows a full-screen notice after sign-up instructing the user to verify email.
 */
public class EmailVerificationInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification_info);

        String displayName = getIntent().getStringExtra("displayName");
        String email = getIntent().getStringExtra("email");

        TextView title = findViewById(R.id.titleTextView);
        TextView message = findViewById(R.id.messageTextView);
        Button openEmailButton = findViewById(R.id.openEmailButton);
        Button goToLoginButton = findViewById(R.id.goToLoginButton);

        title.setText("Verify your email for Aurorus Connect");
        String helloName = displayName == null ? "there" : displayName;
        String emailText = email == null ? "your email" : email;
        message.setText("Hello " + helloName + ",\n\n" +
                "We sent a verification link to " + emailText + ".\n" +
                "Open your inbox and tap the link to verify.\n\n" +
                "If you didn't request this, you can ignore the email.\n\n" +
                "— Your Aurorus Connect team");

        openEmailButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception ignored) { }
        });

        goToLoginButton.setOnClickListener(v -> {
            Intent i = new Intent(EmailVerificationInfoActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}



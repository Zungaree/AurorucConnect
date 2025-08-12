package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView forgotPasswordText, signUpText;
    private Button resendVerificationButton; // optional button in XML
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        // This button should exist in your XML (initially hidden). If not, add it.
        try {
            resendVerificationButton = findViewById(R.id.buttonResendVerification);
            if (resendVerificationButton != null) {
                resendVerificationButton.setVisibility(View.GONE);
                resendVerificationButton.setOnClickListener(v -> resendVerification());
            }
        } catch (Exception ignored) { }
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        signUpText = findViewById(R.id.signUpText);

        // Set up click listeners
        loginButton.setOnClickListener(v -> loginUser());
        forgotPasswordText.setOnClickListener(v -> handleForgotPassword());
        signUpText.setOnClickListener(v -> handleSignUp());

        // Check if user is already logged in
        if (firebaseAuth.getCurrentUser() != null) {
            startMainActivity();
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Show loading state
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Sign in with Firebase
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // Login successful and verified
                                startMainActivity();
                            } else {
                                // Block unverified users
                                Toast.makeText(LoginActivity.this,
                                        "Please verify your email first.",
                                        Toast.LENGTH_LONG).show();
                                if (resendVerificationButton != null) {
                                    resendVerificationButton.setVisibility(View.VISIBLE);
                                }
                                FirebaseAuth.getInstance().signOut();
                                // Reset button state
                                loginButton.setEnabled(true);
                                loginButton.setText("Login");
                            }
                        } else {
                            // Login failed
                            Toast.makeText(LoginActivity.this, 
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                            
                            // Reset button state
                            loginButton.setEnabled(true);
                            loginButton.setText("Login");
                        }
                    }
                });
    }

    // Resend verification email workflow
    private void resendVerification() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter your email and password first.", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && !user.isEmailVerified()) {
                            user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                                if (sendTask.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this,
                                            "Verification email resent.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Failed to resend: " + (sendTask.getException() != null ? sendTask.getException().getMessage() : ""),
                                            Toast.LENGTH_LONG).show();
                                }
                                FirebaseAuth.getInstance().signOut();
                            });
                        } else if (user != null) {
                            Toast.makeText(LoginActivity.this, "This account is already verified.", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Unable to sign in for resend: " + (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleForgotPassword() {
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, 
                            "Password reset email sent", 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, 
                            "Failed to send reset email: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
        finish();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
} 
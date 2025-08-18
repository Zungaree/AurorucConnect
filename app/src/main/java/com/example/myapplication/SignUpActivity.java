package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.util.Base64;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private TextInputEditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signUpButton, selectProfilePictureButton;
    private TextView loginText;
    private ImageView profilePictureImageView;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;
    private Uri selectedImageUri;
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        selectProfilePictureButton = findViewById(R.id.selectProfilePictureButton);
        profilePictureImageView = findViewById(R.id.profilePictureImageView);
        loginText = findViewById(R.id.loginText);

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Set up click listeners
        signUpButton.setOnClickListener(v -> signUpUser());
        selectProfilePictureButton.setOnClickListener(v -> checkPermissionAndPickImage());
        loginText.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void signUpUser() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(firstName)) {
            firstNameEditText.setError("First name is required");
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameEditText.setError("Last name is required");
            return;
        }

        // Combine first and last name
        String fullName = firstName + " " + lastName;

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        // Profile picture is required
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        signUpButton.setEnabled(false);
        signUpButton.setText("Creating Account...");

        // Create user with Firebase Auth, then set displayName and send verification email
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                    // Save user data (async)
                                    saveUserDataWithProfilePicture(fullName, email);
                                    // Immediately navigate to info page (no Toast)
                                    Intent i = new Intent(SignUpActivity.this, EmailVerificationInfoActivity.class);
                                    i.putExtra("displayName", fullName);
                                    i.putExtra("email", email);
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(i);
                                    FirebaseAuth.getInstance().signOut();
                                    finish();
                                });
                            });
                        } else {
                            // Shouldn't happen, but handle gracefully
                            Toast.makeText(SignUpActivity.this,
                                    "User creation succeeded but user is null.",
                                    Toast.LENGTH_LONG).show();
                            resetButtonState();
                        }
                    } else {
                        // Sign up failed
                        Toast.makeText(SignUpActivity.this,
                                "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        
                        // Reset button state
                        signUpButton.setEnabled(true);
                        signUpButton.setText("Sign Up");
                    }
                });
    }

    private void saveUserDataWithProfilePicture(String name, String email) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());

        // Add profile picture as base64 (required)
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            userData.put("profilePicture", base64Image);
        } catch (IOException e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            resetButtonState();
            return;
        }

        usersRef.child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Do nothing; navigation is handled immediately after sending the email
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Failed to save user data: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        resetButtonState();
                    }
                });
    }

    private void resetButtonState() {
        signUpButton.setEnabled(true);
        signUpButton.setText("Sign Up");
    }

    private void initializeActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        profilePictureImageView.setImageBitmap(bitmap);
                        selectProfilePictureButton.setText("Change Profile Picture");
                    } catch (IOException e) {
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
} 
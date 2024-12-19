package com.project.wmpproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    // These are the parts of the screen that the user interacts with
    private ShapeableImageView profileImage; // The user's profile picture
    private TextInputEditText usernameEditText, emailEditText; // Fields to enter username and email
    // Tools for user accounts, database, and image storage
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    // The image selected by the user from their device
    private Uri selectedImageUri;

    // This allows the user to pick an image from their device
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // If the user selects an image, store the image URI and display it
                    selectedImageUri = uri;
                    Picasso.get().load(uri).into(profileImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up the toolbar at the top of the screen
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up Firebase tools
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Connect the code to the elements in the screen design (activity_profile.xml)
        profileImage = findViewById(R.id.profileImage);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        MaterialButton saveButton = findViewById(R.id.saveButton);
        MaterialButton signOutButton = findViewById(R.id.signOutButton);

        // Load the user's profile information
        loadUserProfile();

        // When the profile image is clicked, allow the user to pick an image
        profileImage.setOnClickListener(v -> pickImage.launch("image/*"));

        // When the "Save" button is clicked, save the profile changes
        saveButton.setOnClickListener(v -> saveProfile());

        // When the "Sign Out" button is clicked, sign the user out
        signOutButton.setOnClickListener(v -> signOut());
    }

    // Load the user's profile information from the database
    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Get the user's ID
            String userId = user.getUid();
            // Get the user's document from the database
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // If the user document exists, get the username, email, and profile image URL
                    String username = documentSnapshot.getString("username");
                    String email = documentSnapshot.getString("email");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                    // Set the username and email in the text fields
                    usernameEditText.setText(username);
                    emailEditText.setText(email);

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        // If the user has a profile image, load it from the URL
                        Picasso.get().load(profileImageUrl).into(profileImage);
                    } else {
                        // Otherwise, use a default profile image
                        profileImage.setImageResource(R.drawable.ic_default_profile);
                    }
                }
            }).addOnFailureListener(e ->
                    // If there was an error loading the profile, show an error message
                    Toast.makeText(ProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show()
            );
        }
    }

    // Save the profile changes to the database
    private void saveProfile() {
        // Get the username and email from the text fields
        String username = Objects.requireNonNull(usernameEditText.getText()).toString().trim();
        String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();

        // Check if the user filled in all the fields
        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Get the user's ID
            String userId = user.getUid();
            // Get the user's document from the database
            DocumentReference userRef = db.collection("users").document(userId);

            // Create a map to store the updated data
            Map<String, Object> updates = new HashMap<>();
            updates.put("username", username);
            updates.put("email", email);

            if (selectedImageUri != null) {
                // If the user selected a new profile image, upload it to Firebase Storage
                StorageReference imageRef = storageRef.child("profile_images/" + userId + ".jpg");
                imageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    // After uploading, get the download URL of the image
                                    updates.put("profileImageUrl", uri.toString());
                                    // And update the user's document in Firestore
                                    updateFirestore(userRef, updates);
                                }))
                        .addOnFailureListener(e ->
                                // If there was an error uploading the image, show an error message
                                Toast.makeText(ProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        );
            } else {
                // If the user didn't select a new image, just update the other data in Firestore
                updateFirestore(userRef, updates);
            }
        }
    }

    // Update the user's document in Firestore
    private void updateFirestore(DocumentReference userRef, Map<String, Object> updates) {
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // If the update is successful, show a success message
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // And reload the profile to reflect the changes
                    loadUserProfile();
                })
                .addOnFailureListener(e ->
                        // If there was an error updating the profile, show an error message
                        Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                );
    }

    // Sign the user out
    private void signOut() {
        mAuth.signOut();
        // Go back to the AuthActivity (login screen)
        startActivity(new Intent(this, AuthActivity.class));
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        // Close the current activity
        finish();
    }
}

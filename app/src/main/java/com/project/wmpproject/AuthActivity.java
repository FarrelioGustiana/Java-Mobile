package com.project.wmpproject;

// Importing necessary tools for the screen, buttons, and Firebase
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    // These are the parts of the screen that the user interacts with
    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button registerToggleButton, loginToggleButton;
    private boolean isLoginMode = false; // Keep track of whether the user is logging in or registering
    private LinearLayout usernameField;
    private TextView forgotPasswordTextView;
    private CheckBox termsCheckBox;
    private Button btnSubmit;

    // Tools for user accounts and database
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Connect the code to the elements in the screen design (activity_auth.xml)
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerToggleButton = findViewById(R.id.registerToggleButton);
        loginToggleButton = findViewById(R.id.loginToggleButton);
        usernameField = findViewById(R.id.usernameField);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Set up Firebase tools
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Switch between login and registration modes when buttons are clicked
        registerToggleButton.setOnClickListener(v -> setAuthMode(false)); // Registration mode
        loginToggleButton.setOnClickListener(v -> setAuthMode(true));   // Login mode

        // Do something when the "Submit" button is clicked
        btnSubmit.setOnClickListener(v -> handleSubmit());

        // Start in login mode
        setAuthMode(true);
    }

    // Change the screen to show either login or registration fields
    private void setAuthMode(boolean loginMode) {
        isLoginMode = loginMode;

        if (isLoginMode) {
            // Show login fields and hide registration fields
            registerToggleButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            loginToggleButton.setBackgroundColor(getResources().getColor(android.R.color.black));
            usernameField.setVisibility(View.GONE);
            forgotPasswordTextView.setVisibility(View.VISIBLE);
            termsCheckBox.setVisibility(View.GONE);
            btnSubmit.setText("Login");
        } else {
            // Show registration fields and hide login fields
            registerToggleButton.setBackgroundColor(getResources().getColor(android.R.color.black));
            loginToggleButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            usernameField.setVisibility(View.VISIBLE);
            forgotPasswordTextView.setVisibility(View.GONE);
            termsCheckBox.setVisibility(View.VISIBLE);
            btnSubmit.setText("Register");
        }
    }

    // Handle what happens when the "Submit" button is clicked
    private void handleSubmit() {
        // Get the email and password the user entered
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check if the user filled in all the fields
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLoginMode) {
            // If the user is registering, get the username and check the terms checkbox
            String username = usernameEditText.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!termsCheckBox.isChecked()) {
                Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
                return;
            }
            // Register the user with Firebase
            registerUser(email, password, username.trim());
        } else {
            // If the user is logging in, log them in with Firebase
            userLogin(email, password);
        }
    }

    // Create a new user account in Firebase
    private void registerUser(String email, String password, String username) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // If registration is successful, save the user's information in Firebase
                        String userId = task.getResult().getUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("username", username);
                        userData.put("email", email);
                        userData.put("role", "user");
                        db.collection("users").document(userId).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AuthActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                    // Go to the HomeActivity screen
                                    Intent intent = new Intent(AuthActivity.this, HomeActivity.class);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AuthActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // If registration fails, show an error message
                        Toast.makeText(AuthActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Log in the user with Firebase
    private void userLogin(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // If login is successful, check the user's role (e.g., admin or regular user)
                        String userId = task.getResult().getUser().getUid();
                        checkUserRole(userId);
                    } else {
                        // If login fails, show an error message
                        Toast.makeText(AuthActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Check if the user is an admin or a regular user
    private void checkUserRole(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            // If user data is found, get their role
                            String role = task.getResult().getString("role");
                            if ("admin".equals(role)) {
                                // If the user is an admin, go to the AdminActivity screen
                                startActivity(new Intent(AuthActivity.this, AdminActivity.class));
                            } else {
                                // Otherwise, go to the HomeActivity screen
                                startActivity(new Intent(AuthActivity.this, HomeActivity.class));
                            }
                            finish();
                        } else {
                            // If user data is not found, show an error message
                            Toast.makeText(AuthActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If there was an error fetching user data, show an error message
                        Toast.makeText(AuthActivity.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
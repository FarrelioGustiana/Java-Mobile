package com.project.wmpproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    // This line creates a tool called 'auth'
    // that helps your app handle user logins and accounts.
    private FirebaseAuth auth;
    // This line creates a tool called 'db'
    // that lets your app talk to Firebase and store or retrieve data.
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // This line sets up the 'auth' tool to work with Firebase.
        auth = FirebaseAuth.getInstance();
        // This line sets up the 'db' tool to work with Firebase.
        db = FirebaseFirestore.getInstance();

        // This checks if a user is already logged in.
        // If they are, it calls a function to see what type of user they are
        // (like a regular user or an admin).
        if (auth.getCurrentUser() != null) {
            checkUserRole();
        }

        // This line finds a button in your design ('activity_main.xml')
        // and gives your code a way to control it.
        Button getStartedButton = findViewById(R.id.getStartedButton);
        // This line sets the background color of the button to black.
        getStartedButton.setBackgroundColor(getResources().getColor(android.R.color.black));
        // This tells the app what to do when the "Get Started" button is clicked.
        // In this case, it opens a new screen called 'AuthActivity'
        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            startActivity(intent);
        });
    }

    // This is a separate function that checks the user's role in Firebase.
    private void checkUserRole() {
        // Get the user's ID
        String userId = auth.getCurrentUser().getUid();
        // Access the "users" collection in Firebase and get the user's document
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Get the user's role from the document
                            String role = document.getString("role");
                            if ("admin".equals(role)) {
                                // If the role is "admin", start the AdminActivity
                                startActivity(new Intent(this, AdminActivity.class));
                            } else {
                                // Otherwise, start the HomeActivity
                                startActivity(new Intent(this, HomeActivity.class));
                            }
                            // Close the current activity
                            finish();
                        } else {
                            // Handle the case where the user doesn't exist
                            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle the error
                        Toast.makeText(this, "Error checking user role", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
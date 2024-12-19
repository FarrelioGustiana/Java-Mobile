package com.project.wmpproject;

// Importing necessary tools for the screen, buttons, and Firebase
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.wmpproject.adapter.EventAdapter;
import com.project.wmpproject.model.Event;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    // This is the list of events that will be shown on the screen
    private RecyclerView eventsRecyclerView;
    // Tool for interacting with the database
    private FirebaseFirestore db;
    // Tool for displaying the list of events
    private EventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up the toolbar at the top of the screen
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Connect the code to the elements in the screen design (activity_home.xml)
        ImageView profileImage = findViewById(R.id.profileImage);
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);

        // Set up Firebase tools
        db = FirebaseFirestore.getInstance();

        // Get the list of events from the database
        getEventsFromFirestore();

        // Go to the ProfileActivity when the profile image is clicked
        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Go to the SearchActivity when the search button is clicked
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        // Set up the RecyclerView to display the list of events
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new ArrayList<>()); // Start with an empty list
        eventsRecyclerView.setAdapter(adapter);
    }

    // Get the events from the Firebase database
    private void getEventsFromFirestore() {
        db.collection("events").addSnapshotListener((value, error) -> {
            if (error != null) {
                // If there's an error, print it to the log
                Log.w("HomeActivity", "Listen failed.", error);
                return;
            }

            // Create a list to store the events
            List<Event> events = new ArrayList<>();
            // Go through each event in the database
            for (QueryDocumentSnapshot document : value) {
                // Convert the database data to an Event object
                Event event = document.toObject(Event.class);
                event.setEventId(document.getId());
                // Add the event to the list
                events.add(event);
            }
            // Update the adapter with the new list of events
            adapter.events = events;
            // Tell the adapter to refresh the list on screen
            adapter.notifyDataSetChanged();
        });
    }
}
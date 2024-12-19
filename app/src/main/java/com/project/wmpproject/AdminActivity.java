package com.project.wmpproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.wmpproject.adapter.AdminEventAdapter;
import com.project.wmpproject.model.Event;

import java.util.ArrayList;
import java.util.List;
public class AdminActivity extends AppCompatActivity {

    // This is the list of events that will be shown on the screen
    private RecyclerView eventsRecyclerView;
    // Tool for displaying the list of events (specifically for admins)
    private AdminEventAdapter eventAdapter;
    // A list to store the events
    private List<Event> eventList;
    // Tool for interacting with the database
    private FirebaseFirestore db;
    // Button to add a new event
    private FloatingActionButton addEventButton;
    // Button to sign out
    private Button signOutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up Firebase tools
        db = FirebaseFirestore.getInstance();
        // Connect the code to the elements in the screen design (activity_admin.xml)
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        addEventButton = findViewById(R.id.addEventButton);
        signOutButton = findViewById(R.id.signOutButton);

        // Initialize the list of events and the adapter
        eventList = new ArrayList<>();
        eventAdapter = new AdminEventAdapter(eventList);

        // Set up the RecyclerView to display the list of events
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setAdapter(eventAdapter);

        // Fetch the events from the database
        fetchEvents();

        // Go to the AddEventActivity when the add event button is clicked
        addEventButton.setOnClickListener(view ->
                startActivity(new Intent(AdminActivity.this, AddEventActivity.class))
        );

        // Sign the user out when the sign out button is clicked
        signOutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            // Go back to the MainActivity
            startActivity(new Intent(AdminActivity.this, MainActivity.class));
            // Close the current activity
            finish();
        });
    }

    // Fetch the events from the Firebase database
    private void fetchEvents() {
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
            eventAdapter.events = events;
            // Tell the adapter to refresh the list on screen
            eventAdapter.notifyDataSetChanged();
        });
    }
}
package com.project.wmpproject;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

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

public class SearchActivity extends AppCompatActivity {

    // This is where the user types their search query
    private EditText searchEditText;
    // This is the list of search results that will be shown on the screen
    private RecyclerView searchResultsRecyclerView;
    // Tool for displaying the list of events
    private EventAdapter adapter;
    // Tool for interacting with the database
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Connect the code to the elements in the screen design (activity_search.xml)
        searchEditText = findViewById(R.id.searchEditText);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        // Set up the RecyclerView to display the list of events
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new ArrayList<>()); // Start with an empty list
        searchResultsRecyclerView.setAdapter(adapter);

        // Set up Firebase tools
        db = FirebaseFirestore.getInstance();

        // Listen for changes in the search text field
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used, but required by the TextWatcher interface
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used, but required by the TextWatcher interface
            }

            @Override
            public void afterTextChanged(Editable s) {
                // This function is called after the text in the search field has changed
                // Get the search query from the text field
                String searchQuery = s.toString().trim();
                // Search for events that match the query
                searchEvents(searchQuery);
            }
        });
    }

    // Search for events that match the given query
    private void searchEvents(String searchQuery) {
        // If the search query is empty, clear the list of events
        if (searchQuery.isEmpty()) {
            adapter.events.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        // Get all events from the database
        db.collection("events").addSnapshotListener((value, error) -> {
            if (error != null) {
                // If there's an error, print it to the log
                Log.w("Search Activity", "Listen failed.", error);
                return;
            }

            // Create a list to store the matching events
            List<Event> events = new ArrayList<>();

            // Go through each event in the database
            for (QueryDocumentSnapshot document : value) {
                // Convert the database data to an Event object
                Event event = document.toObject(Event.class);
                event.setEventId(document.getId());
                // Check if the event title or description contains the search query (case-insensitive)
                if (event.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                        event.getDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                    // If it does, add the event to the list
                    events.add(event);
                }
            }

            // Update the adapter with the new list of events
            adapter.events = events;
            // Tell the adapter to refresh the list on screen
            adapter.notifyDataSetChanged();
        });
    }
}
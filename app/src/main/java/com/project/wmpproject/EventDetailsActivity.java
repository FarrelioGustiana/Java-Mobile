package com.project.wmpproject;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.wmpproject.adapter.AttendanceAdapter;
import com.project.wmpproject.model.Attendance;
import com.project.wmpproject.model.Event;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {

    // These are the parts of the screen that show event details
    private ImageView eventImage;
    private TextView eventTitle, eventDescription, eventDateTime, eventLocation, attendanceLimitText;
    private Button checkInButton;
    // This is the list of attendees that will be shown on the screen
    private RecyclerView attendanceRecyclerView;
    // Tool for displaying the list of attendees
    private AttendanceAdapter attendanceAdapter;

    // Tools for interacting with the database and user accounts
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    // The ID of the event being displayed
    private String eventId;
    // The event object itself
    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This function is called when the activity starts
        super.onCreate(savedInstanceState);
        // Make app screen edge-to-edge
        EdgeToEdge.enable(this);
        // Use the design from activity_event_details.xml
        setContentView(R.layout.activity_event_details);

        // Connect the code to the elements in the screen design (activity_event_details.xml)
        eventImage = findViewById(R.id.eventImage);
        eventTitle = findViewById(R.id.eventTitle);
        eventDescription = findViewById(R.id.eventDescription);
        attendanceLimitText = findViewById(R.id.attendanceLimitText);
        eventDateTime = findViewById(R.id.eventDateTime);
        eventLocation = findViewById(R.id.eventLocation);
        checkInButton = findViewById(R.id.checkInButton);
        attendanceRecyclerView = findViewById(R.id.attendanceRecyclerView);
        attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up Firebase tools
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get the event ID from the intent that started this activity
        eventId = getIntent().getStringExtra("eventId");

        if (eventId != null) {
            // If the event ID is valid, fetch the event data from the database
            fetchEventData(eventId);
        } else {
            // If the event ID is invalid, show an error message and close the activity
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        // When the "Check In" button is clicked, check the user in to the event
        checkInButton.setOnClickListener(v -> checkInToEvent());
    }

    // Get the event data from the database
    private void fetchEventData(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If the data is fetched successfully, convert it to an Event object
                        event = task.getResult().toObject(Event.class);
                        if (event != null) {
                            // If the event exists, display its data on the screen
                            displayEventData(event);
                            // And fetch the attendance data for this event
                            fetchAttendanceData(eventId);
                        } else {
                            // If the event doesn't exist, show an error message and close the activity
                            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        // If there was an error fetching the data, show an error message and close the activity
                        Log.w("EventDetailsActivity", "Error fetching event data", task.getException());
                        Toast.makeText(this, "Error fetching event data", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    // Display the event data on the screen
    private void displayEventData(Event event) {
        Picasso.get().load(event.getImageUrl()).into(eventImage);
        eventTitle.setText(event.getTitle());
        eventDescription.setText(event.getDescription());
        eventDateTime.setText(event.getDateTime());
        eventLocation.setText(event.getLocation());
        int limit = event.getAttendanceLimit();
        attendanceLimitText.setText("Attendance Limit: " + limit);
    }

    // Check the user in to the event
    private void checkInToEvent() {
        if (event == null) {
            // If the event object is null, do nothing
            return;
        }

        int attendanceLimit = event.getAttendanceLimit();

        // Check the current number of attendees
        db.collection("events").document(eventId).collection("attendance")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int currentAttendance = task.getResult().size();
                        if (currentAttendance >= attendanceLimit) {
                            Toast.makeText(this, "Event attendance limit reached.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Proceed with the existing check-in logic
                            String userId = auth.getCurrentUser().getUid();
                            db.collection("events").document(eventId).collection("attendance")
                                    .whereEqualTo("userId", userId)
                                    .get()
                                    .addOnCompleteListener(task2 -> {
                                        if (task2.isSuccessful()) {
                                            if (task2.getResult().isEmpty()) {
                                                performCheckIn(userId);
                                            } else {
                                                Toast.makeText(this, "You have already checked in to this event.", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Log.w("EventDetailsActivity", "Error checking attendance", task2.getException());
                                            Toast.makeText(this, "Error checking attendance", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Log.w("EventDetailsActivity", "Error getting attendance count", task.getException());
                        Toast.makeText(this, "Error checking attendance", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Perform the actual check-in process
    private void performCheckIn(String userId) {
        // Get the event's date and time
        String dateTime = event.getDateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        try {
            // Convert the event's date and time to a Date object
            Date eventDate = sdf.parse(dateTime);
            // Get the current date and time
            Date currentDate = new Date();

            // Check if the current time is after the event's scheduled time
            if (currentDate.before(eventDate)) {
                // If it's not, show a message and don't allow check-in
                Toast.makeText(this, "You can only check in after the scheduled time.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the subcollection where attendance data is stored
            CollectionReference attendanceRef = db.collection("events").document(eventId).collection("attendance");

            // Create a map to store the attendance data
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("userId", userId); // Add the user's ID
            attendanceData.put("checkinTime", FieldValue.serverTimestamp()); // Add the check-in time

            // Add a new document to the attendance subcollection
            attendanceRef.add(attendanceData)
                    .addOnSuccessListener(docRef -> {
                        // If the check-in is successful, show a message
                        Toast.makeText(EventDetailsActivity.this, "Checked in successfully!", Toast.LENGTH_SHORT).show();
                        // And refresh the attendance list
                        fetchAttendanceData(eventId);
                    })
                    .addOnFailureListener(e -> {
                        // If there was an error checking in, show an error message
                        Log.w("EventDetailsActivity", "Error checking in", e);
                        Toast.makeText(EventDetailsActivity.this, "Error checking in", Toast.LENGTH_SHORT).show();
                    });

        } catch (ParseException e) {
            // If there was an error parsing the date, show an error message
            Log.e("EventDetailsActivity", "Error parsing date", e);
            Toast.makeText(this, "Error parsing date", Toast.LENGTH_SHORT).show();
        }
    }

    // Get the attendance data from the database
    private void fetchAttendanceData(String eventId) {
        db.collection("events").document(eventId).collection("attendance")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If the data is fetched successfully, create a list to store it
                        List<Attendance> attendanceData = new ArrayList<>();
                        // Go through each attendance record
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Get the user ID and check-in time
                            String userId = document.getString("userId");
                            Timestamp checkinTimestamp = document.getTimestamp("checkinTime");
                            Timestamp checkinTime = checkinTimestamp != null ? checkinTimestamp : null;
                            // Create an Attendance object and add it to the list
                            attendanceData.add(new Attendance(userId, checkinTime));
                        }
                        // Populate the attendance list on the screen
                        populateAttendanceList(attendanceData);
                    } else {
                        // If there was an error fetching the data, show an error message
                        Log.w("EventDetailsActivity", "Error fetching attendance data", task.getException());
                        Toast.makeText(this, "Error fetching attendance data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Populate the attendance list on the screen
    private void populateAttendanceList(List<Attendance> attendanceData) {
        attendanceAdapter = new AttendanceAdapter(attendanceData, db);
        attendanceRecyclerView.setAdapter(attendanceAdapter);
    }
}
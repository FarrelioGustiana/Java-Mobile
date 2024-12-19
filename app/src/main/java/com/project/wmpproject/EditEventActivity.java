package com.project.wmpproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.wmpproject.model.Event;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditEventActivity extends AppCompatActivity {

    // These are the parts of the screen that the user interacts with
    private TextInputEditText titleEditText, descriptionEditText, locationEditText, attendanceLimitEditText; // Fields to enter event details
    private ImageView eventImageView; // To display the event image
    private Button saveChangesButton; // Button to save changes
    // Tool for interacting with the database
    private FirebaseFirestore db;
    // The ID of the event being edited
    private String eventId;
    // The image selected by the user from their device
    private Uri selectedImageUri;
    // This allows the user to pick an image from their device
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    // The selected date and time
    private TextInputEditText dateEditText, timeEditText;
    private Calendar selectedDate;
    // The original image URL of the event
    private String prevImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up Firebase tools
        db = FirebaseFirestore.getInstance();
        // Get the event ID from the intent that started this activity
        eventId = getIntent().getStringExtra("eventId");

        // Connect the code to the elements in the screen design (activity_edit_event.xml)
        titleEditText = findViewById(R.id.editEventTitle);
        descriptionEditText = findViewById(R.id.editEventDescription);
        dateEditText = findViewById(R.id.addEventDate);
        timeEditText = findViewById(R.id.addEventTime);
        attendanceLimitEditText = findViewById(R.id.editEventAttendanceLimit);
        locationEditText = findViewById(R.id.editEventLocation);
        eventImageView = findViewById(R.id.editEventImage);
        saveChangesButton = findViewById(R.id.saveChangesButton);

        // Initialize the ActivityResultLauncher for picking an image
        pickMedia = registerForActivityResult(new PickVisualMedia(), uri -> {
            if (uri != null) {
                // If the user selects an image, store the image URI and display it
                Log.d("PhotoPicker", "Selected URI: " + uri);
                selectedImageUri = uri;
                eventImageView.setImageURI(uri);
            } else {
                // If no image is selected, log a message
                Log.d("PhotoPicker", "No media selected");
            }
        });

        // Fetch the event data from the database
        fetchEventData(eventId);

        // Allow the user to pick an image when the image view is clicked
        eventImageView.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        // When the "Save Changes" button is clicked, save the changes to the event
        saveChangesButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                // If a new image is selected, upload it and then save the changes
                uploadImageAndSaveChanges();
            } else {
                // Otherwise, just save the changes with the existing image
                saveChanges();
            }
        });

        // Initialize the selected date with the current date
        selectedDate = Calendar.getInstance();
        // Show the date and time picker dialog when the date/time field is clicked
        dateEditText.setOnClickListener(v -> showDatePickerDialog());
        timeEditText.setOnClickListener(v -> showTimePickerDialog());

    }

    // Show the date and time picker dialog
    private void showDatePickerDialog() {
        int year = selectedDate.get(Calendar.YEAR);
        int month = selectedDate.get(Calendar.MONTH);
        int day = selectedDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year1);
                    selectedDate.set(Calendar.MONTH, monthOfYear);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateEditText();
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        int hour = selectedDate.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDate.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDate.set(Calendar.MINUTE, minute1);
                    updateTimeEditText();
                }, hour, minute, true); // true for 24-hour format
        timePickerDialog.show();
    }

    // Update the date/time text field with the formatted date and time
    private void updateDateEditText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(selectedDate.getTime());
        dateEditText.setText(formattedDate);
    }

    private void updateTimeEditText() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String formattedTime = timeFormat.format(selectedDate.getTime());
        timeEditText.setText(formattedTime);
    }

    // Fetch the event data from the database
    private void fetchEventData(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If the data is fetched successfully, convert it to an Event object
                        Event event = task.getResult().toObject(Event.class);
                        if (event != null) {
                            // If the event exists, populate the fields with its data
                            populateEventFields(event);
                        } else {
                            // If the event doesn't exist, log a message and close the activity
                            Log.d("EditEventActivity", "Event not found");
                            finish();
                        }
                    } else {
                        // If there was an error fetching the data, log the error
                        Log.w("EditEventActivity", "Error fetching event data", task.getException());
                    }
                });
    }

    // Populate the fields with the event data
    private void populateEventFields(Event event) {
        titleEditText.setText(event.getTitle());
        descriptionEditText.setText(event.getDescription());
        locationEditText.setText(event.getLocation());
        attendanceLimitEditText.setText(String.valueOf(event.getAttendanceLimit()));

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(event.getDateTime());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            dateEditText.setText(dateFormat.format(date));
            timeEditText.setText(timeFormat.format(date));
        } catch (ParseException e) {
            Log.e("EditEventActivity", "Error parsing date", e);
            Toast.makeText(this, "Error parsing date", Toast.LENGTH_SHORT).show();
        }
        // Store the original image URL
        this.prevImageUrl = event.getImageUrl();
        // Load the image into the image view
        Picasso.get().load(event.getImageUrl()).into(eventImageView);
    }

    // Upload the new image to Firebase Storage and then save the changes to the event
    private void uploadImageAndSaveChanges() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        // Create a unique name for the image
        String imageName = "event_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        StorageReference imageRef = storageRef.child("events/" + imageName);

        // Upload the image to Firebase Storage
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        // If the upload is successful, get the download URL of the image
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Update the event with the new image URL and other details
                            String imageUrl = uri.toString();
                            String title = titleEditText.getText().toString();
                            String description = descriptionEditText.getText().toString();
                            String dateTime = dateEditText.getText().toString() + "T" + timeEditText.getText().toString();
                            String location = locationEditText.getText().toString();

                            int attendanceLimit = Integer.parseInt(attendanceLimitEditText.getText().toString());

                            Event updatedEvent = new Event(title, description, dateTime, location, imageUrl, eventId, attendanceLimit);

                            // Update the event in Firestore
                            db.collection("events").document(eventId)
                                    .set(updatedEvent)
                                    .addOnSuccessListener(aVoid -> {
                                        // If the update is successful, show a success message and close the activity
                                        Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            // If there was an error updating the event, log the error
                                            Log.w("EditEventActivity", "Error updating event", e)
                                    );
                        })
                )
                .addOnFailureListener(e -> {
                    // If there was an error uploading the image, show an error message
                    Log.w("EditEventActivity", "Error uploading image", e);
                    Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show();
                });
    }

    // Save the changes to the event without uploading a new image
    private void saveChanges() {
        // Get the event details from the text fields
        String title = titleEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        String dateTime = dateEditText.getText().toString() + "T" + timeEditText.getText().toString();
        String location = locationEditText.getText().toString();
        int attendanceLimit = Integer.parseInt(attendanceLimitEditText.getText().toString());
        // Use the existing image URL
        String imageUrl = prevImageUrl;

        // Create a new Event object with the updated details
        Event updatedEvent = new Event(title, description, dateTime, location, imageUrl, eventId, attendanceLimit);

        // Update the event in Firestore
        db.collection("events").document(eventId)
                .set(updatedEvent)
                .addOnSuccessListener(aVoid -> {
                    // If the update is successful, show a success message and close the activity
                    Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        // If there was an error updating the event, log the error
                        Log.w("EditEventActivity", "Error updating event", e)
                );
    }
}
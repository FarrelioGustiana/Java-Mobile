package com.project.wmpproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.wmpproject.model.Event;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AddEventActivity extends AppCompatActivity {

    // These are the parts of the screen that the user interacts with
    private EditText titleEditText, descriptionEditText, locationEditText, attendanceLimitEditText;; // Fields to enter event details
    private ImageView eventImageView; // To display the selected image
    private Button addButton; // Button to add the event
    // Tool for interacting with the database
    private FirebaseFirestore db;
    // The image selected by the user from their device
    private Uri selectedImageUri;
    // This allows the user to pick an image from their device
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    // Fields to enter the date and time of the event
    private EditText dateEditText, timeEditText;
    // The selected date and time
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up Firebase tools
        db = FirebaseFirestore.getInstance();

        // Connect the code to the elements in the screen design (activity_add_event.xml)
        titleEditText = findViewById(R.id.addEventTitle);
        descriptionEditText = findViewById(R.id.addEventDescription);
        dateEditText = findViewById(R.id.addEventDate);
        timeEditText = findViewById(R.id.addEventTime);
        attendanceLimitEditText = findViewById(R.id.addEventAttendanceLimit);
        locationEditText = findViewById(R.id.addEventLocation);
        eventImageView = findViewById(R.id.addEventImage);
        addButton = findViewById(R.id.addEventButton);

        // Set a default image for the event image view
        eventImageView.setImageResource(R.drawable.ic_camera);

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

        // Initialize the selected date with the current date
        selectedDate = Calendar.getInstance();

        // Show the date picker dialog when the date field is clicked
        dateEditText.setOnClickListener(v -> showDatePickerDialog());
        // Show the time picker dialog when the time field is clicked
        timeEditText.setOnClickListener(v -> showTimePickerDialog());

        // Allow the user to pick an image when the image view is clicked
        eventImageView.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        // When the "Add" button is clicked, upload the image and add the event to Firestore
        addButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                // If an image is selected, upload it and add the event
                uploadImageAndAddEvent();
            } else {
                // If no image is selected, show a message
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Show the date picker dialog
    private void showDatePickerDialog() {
        // Get the current year, month, and day
        int year = selectedDate.get(Calendar.YEAR);
        int month = selectedDate.get(Calendar.MONTH);
        int day = selectedDate.get(Calendar.DAY_OF_MONTH);

        // Create a new date picker dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // When a date is selected, update the selected date
                    selectedDate.set(Calendar.YEAR, year1);
                    selectedDate.set(Calendar.MONTH, monthOfYear);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    // And update the date text field
                    updateDateEditText();
                }, year, month, day);
        // Show the dialog
        datePickerDialog.show();
    }

    // Show the time picker dialog
    private void showTimePickerDialog() {
        // Get the current hour and minute
        int hour = selectedDate.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDate.get(Calendar.MINUTE);

        // Create a new time picker dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    // When a time is selected, update the selected time
                    selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDate.set(Calendar.MINUTE, minute1);
                    // And update the time text field
                    updateTimeEditText();
                }, hour, minute, true); // true for 24-hour format
        // Show the dialog
        timePickerDialog.show();
    }

    // Update the date text field with the formatted date
    private void updateDateEditText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(selectedDate.getTime());
        dateEditText.setText(formattedDate);
    }

    // Update the time text field with the formatted time
    private void updateTimeEditText() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String formattedTime = timeFormat.format(selectedDate.getTime());
        timeEditText.setText(formattedTime);
    }

    // Upload the image to Firebase Storage and then add the event to Firestore
    private void uploadImageAndAddEvent() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        // Create a unique name for the image
        String imageName = "event_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        StorageReference imageRef = storageRef.child("events/" + imageName);

        // Upload the image to Firebase Storage
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        // If the upload is successful, get the download URL of the image
                        imageRef.getDownloadUrl().addOnSuccessListener(this::addEventToFirestore)
                )
                .addOnFailureListener(e -> {
                    // If there was an error uploading the image, show an error message
                    Log.w("AddEventActivity", "Error uploading image", e);
                    Toast.makeText(this, "Error uploading image", Toast.LENGTH_SHORT).show();
                });
    }

    // Add the event to Firestore
    private void addEventToFirestore(Uri uri) {
        // Get the download URL of the image
        String imageUrl = uri.toString();
        // Get the event details from the text fields
        String title = titleEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        // Combine the date and time into a single string
        String dateTime = dateEditText.getText().toString() + "T" + timeEditText.getText().toString();
        String location = locationEditText.getText().toString();

        // Generate a unique ID for the event
        String eventId = UUID.randomUUID().toString();
        int attendanceLimit = Integer.parseInt(attendanceLimitEditText.getText().toString());

        // Create a new Event object with the details
        Event newEvent = new Event(title, description, dateTime, location, imageUrl, eventId, attendanceLimit);

        // Add the event to the "events" collection in Firestore
        db.collection("events")
                .document(eventId)
                .set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    // If the event is added successfully, show a success message and close the activity
                    Toast.makeText(this, "Event added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // If there was an error adding the event, show an error message
                    Log.w("AddEventActivity", "Error adding event", e);
                    Toast.makeText(this, "Error adding event", Toast.LENGTH_SHORT).show();
                });
    }
}
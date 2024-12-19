package com.project.wmpproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.wmpproject.R;
import com.project.wmpproject.model.Attendance;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    // This is a list of attendees to be displayed
    private List<Attendance> attendanceList;
    // Tool for interacting with the database
    private FirebaseFirestore db;
    // Tool for formatting the time
    private SimpleDateFormat timeFormat;

    // This creates an AttendanceAdapter with the given list of attendees and database
    public AttendanceAdapter(List<Attendance> attendanceList, FirebaseFirestore db) {
        this.attendanceList = attendanceList;
        this.db = db;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    // This creates a view holder for each attendee in the list
    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    // This sets the information for each attendee in the list
    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Attendance attendance = attendanceList.get(position);
        String userId = attendance.userId;

        // Get the user's information from the database
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // If the user exists, get their username and profile image
                String username = doc.getString("username");
                String profileImage = doc.getString("profileImageUrl");
                // Get the check-in time
                Timestamp checkInTime = attendance.checkinTime;

                // Set the username on the screen
                holder.usernameTextView.setText(username);

                if (profileImage != null) {
                    // If the user has a profile image, load it from the URL
                    Picasso.get()
                            .load(profileImage)
                            .placeholder(R.drawable.ic_default_profile) // Use a placeholder image while loading
                            .error(R.drawable.ic_default_profile) // Use a default image if there's an error loading
                            .into(holder.profileImageView);
                }

                if (checkInTime != null) {
                    // If the user has checked in, format the check-in time and display it
                    Date checkInDate = checkInTime.toDate();
                    String formattedTime = timeFormat.format(checkInDate);
                    holder.checkinTimeTextView.setText("Checked in at " + formattedTime);
                }
            }
        }).addOnFailureListener(e -> {
            // If there was an error fetching user data, show an error message
            Toast.makeText(holder.itemView.getContext(),
                    "Error fetching user data: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    // This returns the number of attendees in the list
    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    // This class holds the views for each attendee in the list
    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView;
        TextView checkinTimeTextView;

        // This connects the code to the elements in the item_attendance.xml design
        AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            checkinTimeTextView = itemView.findViewById(R.id.checkinTimeTextView);
        }
    }
}
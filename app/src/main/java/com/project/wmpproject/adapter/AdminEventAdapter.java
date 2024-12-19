    package com.project.wmpproject.adapter;

    import android.content.Intent;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.ImageView;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.recyclerview.widget.RecyclerView;

    import com.google.firebase.firestore.FirebaseFirestore;
    import com.project.wmpproject.EditEventActivity;
    import com.project.wmpproject.R;
    import com.project.wmpproject.model.Event;
    import com.squareup.picasso.Picasso;

    import java.util.List;

    public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.EventViewHolder> {

        // This is a list of events to be displayed
        public List<Event> events;
        // Tool for interacting with the database
        private FirebaseFirestore db;

        // Empty constructor
        public AdminEventAdapter() {
        }

        // Constructor that takes a list of events
        public AdminEventAdapter(List<Event> events) {
            this.events = events;
            this.db = FirebaseFirestore.getInstance();
        }

        // Creates a view holder for each event in the list
        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_admin, parent, false);
            return new EventViewHolder(view);
        }

        // Sets the information for each event in the list
        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            Event event = events.get(position);

            // Load the image from the URL
            Picasso.get().load(event.getImageUrl()).into(holder.eventImage);
            // Set the text for the title, description, date/time, and location
            holder.eventTitle.setText(event.getTitle());
            holder.eventDescription.setText(event.getDescription());
            holder.eventDateTime.setText(event.getDateTime());
            holder.eventLocation.setText(event.getLocation());

            // Go to the EditEventActivity when the "Edit" button is clicked
            holder.editButton.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), EditEventActivity.class);
                intent.putExtra("eventId", event.getEventId());
                v.getContext().startActivity(intent);
            });

            // Delete the event when the "Delete" button is clicked
            holder.deleteButton.setOnClickListener(v -> deleteEvent(event.getEventId(), position));
        }

        // Returns the number of events in the list
        @Override
        public int getItemCount() {
            return events.size();
        }

        // Deletes the event with the given ID from the database
        private void deleteEvent(String eventId, int position) {
            db.collection("events").document(eventId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // If the event is deleted successfully, remove it from the list and update the screen
                        events.remove(position);
                        notifyItemRemoved(position);
                    })
                    .addOnFailureListener(e ->
                            // If there was an error deleting the event, print the error to the log
                            Log.w("EventAdapter", "Error deleting event", e)
                    );
        }

        // This class holds the views for each event in the list
        static class EventViewHolder extends RecyclerView.ViewHolder {
            ImageView eventImage;
            TextView eventTitle, eventDescription, eventDateTime, eventLocation;
            Button editButton, deleteButton;

            // Connects the code to the elements in the item_event_admin.xml design
            EventViewHolder(@NonNull View itemView) {
                super(itemView);
                eventImage = itemView.findViewById(R.id.eventImage);
                eventTitle = itemView.findViewById(R.id.eventTitle);
                eventDescription = itemView.findViewById(R.id.eventDescription);
                eventDateTime = itemView.findViewById(R.id.eventDateTime);
                eventLocation = itemView.findViewById(R.id.eventLocation);
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
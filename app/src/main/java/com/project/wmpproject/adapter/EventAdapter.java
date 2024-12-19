package com.project.wmpproject.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.wmpproject.EventDetailsActivity;
import com.project.wmpproject.R;
import com.project.wmpproject.model.Event;
import com.squareup.picasso.Picasso;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    // This is a list of events to be displayed
    public List<Event> events;

    // This creates an EventAdapter with the given list of events
    public EventAdapter(List<Event> events) {
        this.events = events;
    }

    // This creates a view holder for each event in the list
    @NonNull @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    // This sets the information for each event in the list
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    // This returns the number of events in the list
    @Override
    public int getItemCount() {
        return events.size();
    }

    // This class holds the views for each event in the list
    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImage;
        TextView eventTitle, eventDescription, eventDateTime, eventLocation;
        Button viewDetailsButton;

        // This connects the code to the elements in the item_event.xml design
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.eventImage);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventDescription = itemView.findViewById(R.id.eventDescription);
            eventDateTime = itemView.findViewById(R.id.eventDateTime);
            eventLocation = itemView.findViewById(R.id.eventLocation);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }

        // This sets the information for each event in the view holder
        void bind(Event event) {
            // Load the image from the URL
            Picasso.get().load(event.getImageUrl()).into(eventImage);
            // Set the text for the title, description, date/time, and location
            eventTitle.setText(event.getTitle());
            eventDescription.setText(event.getDescription());
            eventDateTime.setText(event.getDateTime());
            eventLocation.setText(event.getLocation());

            // Go to the EventDetailsActivity when the "View Details" button is clicked
            viewDetailsButton.setOnClickListener(v -> {
                String eventId = event.getEventId();
                Intent intent = new Intent(v.getContext(), EventDetailsActivity.class);
                intent.putExtra("eventId", eventId);
                v.getContext().startActivity(intent);
            });
        }
    }
}
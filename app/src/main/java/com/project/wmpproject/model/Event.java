package com.project.wmpproject.model;

public class Event {
    // These are variables to store information about an event
    private String title;         // The name of the event
    private String description;  // A short text about the event
    private String dateTime;     // When the event happens
    private String location;     // Where the event happens
    private String imageUrl;     // A link to a picture of the event
    private String eventId;      // A unique ID for the event
    private int attendanceLimit;

    // This is like an empty container for an event
    public Event() {}

    // This creates an event with the given information
    public Event(String title, String description, String dateTime, String location, String imageUrl) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    // This creates an event with all the information, including the event ID
    public Event(String title, String description, String dateTime, String location, String imageUrl, String eventId) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.location = location;
        this.imageUrl = imageUrl;
        this.eventId = eventId;
    }

    public Event(String title, String description, String dateTime, String location, String imageUrl, String eventId, int attendanceLimit) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.location = location;
        this.imageUrl = imageUrl;
        this.eventId = eventId;
        this.attendanceLimit = attendanceLimit;
    }

    // This sets the event ID
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // These functions allow other parts of the code to get the information about the event
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDateTime() { return dateTime; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
    public String getEventId() { return eventId; }
    public int getAttendanceLimit() { return attendanceLimit; }
}
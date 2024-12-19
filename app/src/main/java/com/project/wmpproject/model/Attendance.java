package com.project.wmpproject.model;

import com.google.firebase.Timestamp;

public class Attendance {
    public String userId;
    public Timestamp checkinTime;

    public Attendance() {}

    public Attendance(String userId, Timestamp checkinTime) {
        this.userId = userId;
        this.checkinTime = checkinTime;
    }

}
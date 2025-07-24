package com.example.myapplication;

public class CheckInHistory {
    private String timestamp;
    private String userId;
    private String userName;

    public CheckInHistory(String timestamp, String userId, String userName) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.userName = userName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
} 
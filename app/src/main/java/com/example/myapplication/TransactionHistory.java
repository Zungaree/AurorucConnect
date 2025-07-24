package com.example.myapplication;

public class TransactionHistory {
    private String creditsReceived;
    private Long newTotalCredits;
    private String timestamp;

    public TransactionHistory(String creditsReceived, Long newTotalCredits, String timestamp) {
        this.creditsReceived = creditsReceived;
        this.newTotalCredits = newTotalCredits;
        this.timestamp = timestamp;
    }

    public String getCreditsReceived() {
        return creditsReceived;
    }

    public Long getNewTotalCredits() {
        return newTotalCredits;
    }

    public String getTimestamp() {
        return timestamp;
    }
} 
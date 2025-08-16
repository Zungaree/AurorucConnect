package com.example.myapplication;

public class HorizontalCardItem {
    private final String title;
    private final int imageResId;

    public HorizontalCardItem(String title, int imageResId) {
        this.title = title;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResId() {
        return imageResId;
    }
}



package com.example.myapplication;

public class RssItem {
    private String title;
    private String description;
    private String imageUrl;
    private String link;

    public RssItem(String title, String description, String imageUrl, String link) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
} 
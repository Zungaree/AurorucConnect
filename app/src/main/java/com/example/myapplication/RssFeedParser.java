package com.example.myapplication;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RssFeedParser {
    private static final String TAG = "RssFeedParser";
    private final OkHttpClient client;

    public RssFeedParser() {
        this.client = new OkHttpClient();
    }

    public interface RssFeedCallback {
        void onSuccess(List<RssItem> items);
        void onError(String error);
    }

    public void fetchRssFeed(String url, RssFeedCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "AurorusConnect/1.0 (+https://example.com)")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP Error: " + response.code());
                        return;
                    }

                    String xmlContent = response.body().string();
                    List<RssItem> items = parseRssXml(xmlContent);
                    callback.onSuccess(items);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error fetching RSS feed", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error parsing RSS feed", e);
                callback.onError("Parsing error: " + e.getMessage());
            }
        }).start();
    }

    private List<RssItem> parseRssXml(String xmlContent) {
        List<RssItem> items = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser());
            Elements itemElements = doc.select("item");

            for (Element item : itemElements) {
                String title = item.select("title").text();
                String description = item.select("description").text();
                String link = item.select("link").text();
                
                // Try to get image from enclosure tag
                String imageUrl = null;
                Element enclosure = item.select("enclosure[type^=image]").first();
                if (enclosure != null) {
                    imageUrl = enclosure.attr("url");
                }
                
                // If no enclosure, try to extract from description
                if (imageUrl == null || imageUrl.isEmpty()) {
                    imageUrl = extractImageFromDescription(description);
                }

                RssItem rssItem = new RssItem(title, description, imageUrl, link);
                items.add(rssItem);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing XML", e);
        }

        return items;
    }

    private String extractImageFromDescription(String description) {
        try {
            Document descDoc = Jsoup.parse(description);
            Element img = descDoc.select("img").first();
            if (img != null) {
                return img.attr("src");
            }
        } catch (Exception e) {
            Log.d(TAG, "Could not extract image from description", e);
        }
        return null;
    }
} 
package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class CatalogActivity extends AppCompatActivity {
    private static final String RSS_FEED_URL = "https://fetchrss.com/feed/aJDkepJhFR3DaJh8PqZvX-IC.rss";
    
    private RecyclerView recyclerView;
    private RssItemAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RssFeedParser rssFeedParser;
    private List<RssItem> rssItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Initialize views
        recyclerView = findViewById(R.id.rssRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Initialize data
        rssItems = new ArrayList<>();
        rssFeedParser = new RssFeedParser();

        // Setup RecyclerView
        adapter = new RssItemAdapter(rssItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadRssFeed);

        // Load RSS feed
        loadRssFeed();
    }

    private void loadRssFeed() {
        showLoading(true);
        
        rssFeedParser.fetchRssFeed(RSS_FEED_URL, new RssFeedParser.RssFeedCallback() {
            @Override
            public void onSuccess(List<RssItem> items) {
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    
                    if (items.isEmpty()) {
                        showEmptyState("No posts found in the feed");
                    } else {
                        hideEmptyState();
                        rssItems.clear();
                        rssItems.addAll(items);
                        adapter.updateItems(rssItems);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    showEmptyState("Error loading feed: " + error);
                    Snackbar.make(recyclerView, "Error: " + error, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyStateTextView.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.VISIBLE);
        emptyStateTextView.setText(message);
    }

    private void hideEmptyState() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateTextView.setVisibility(View.GONE);
    }
} 
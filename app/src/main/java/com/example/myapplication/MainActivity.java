package com.example.myapplication;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private NfcAdapter nfcAdapter;
    private TextView welcomeTextView;
    private TextView membershipStatusText;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView feedRecyclerView;
    private RecyclerView productsRecyclerView;
    private LinearSnapHelper snapHelper;
    private LinearSnapHelper productsSnapHelper;
    private Handler autoScrollHandler;
    private Handler productsAutoScrollHandler;
    private int autoScrollPosition = 0;
    private int productsAutoScrollPosition = 0;
    private static final int AUTO_SCROLL_INTERVAL_MS = 3000;
    private ComponentName hceService;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;
    
    // BroadcastReceiver to handle NFC status updates and logs from our service
    private BroadcastReceiver nfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyHostApduService.ACTION_NFC_STATUS.equals(action)) {
                boolean connected = intent.getBooleanExtra(MyHostApduService.EXTRA_NFC_CONNECTED, false);
                if (connected) {
                    Toast.makeText(MainActivity.this, "NFC reader connected!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        // Initialize views
        welcomeTextView = findViewById(R.id.welcomeTextView);
        membershipStatusText = findViewById(R.id.membershipStatusText);
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
        feedRecyclerView = findViewById(R.id.feedRecyclerView);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        
        // Initialize HCE service component
        hceService = new ComponentName(this, MyHostApduService.class);
        
        // Set up bottom navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_check_in) {
                startCheckIn();
                return true;
            } else if (itemId == R.id.nav_products) {
                openProducts();
                return true;
            } else if (itemId == R.id.nav_battle_pass) {
                openBattlePass();
                return true;
            } else if (itemId == R.id.nav_store) {
                openStoreCredits();
                return true;
            } else if (itemId == R.id.nav_account) {
                openAccount();
                return true;
            }
            return false;
        });

        // Set up Aurorus card click listener for check-in
        findViewById(R.id.aurorusCard).setOnClickListener(v -> startCheckIn());
        
        // Attach snap helper for paging-like snapping
        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(feedRecyclerView);
        
        productsSnapHelper = new LinearSnapHelper();
        productsSnapHelper.attachToRecyclerView(productsRecyclerView);
        
        // Initialize NFC
        initNfc();
        
        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Update user info
        updateUserInfo();
        
        // Setup carousels using RSS data
        fetchAndBindRssCarousels();
        
        // Setup products carousel
        fetchAndBindProductsCarousel();

        // Ensure HCE service is stopped initially
        stopHceService();

        // Init auto-scroll handlers
        autoScrollHandler = new Handler(getMainLooper());
        productsAutoScrollHandler = new Handler(getMainLooper());
    }

    private void fetchAndBindRssCarousels() {
        // Horizontal layout managers
        LinearLayoutManager horizontalLm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        feedRecyclerView.setLayoutManager(horizontalLm);
        // Ensure first item is centered using snap helper already attached
        feedRecyclerView.post(() -> feedRecyclerView.smoothScrollToPosition(0));

        RssFeedParser parser = new RssFeedParser();
        String feedUrl = getString(R.string.rss_feed_url);
        parser.fetchRssFeed(feedUrl, new RssFeedParser.RssFeedCallback() {
            @Override
            public void onSuccess(java.util.List<RssItem> items) {
                runOnUiThread(() -> {
                    // Use same handling as Catalog: if empty, show empty state-like toasts and skip
                    if (items == null || items.isEmpty()) {
                        Toast.makeText(MainActivity.this, getString(R.string.no_posts_available), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    feedRecyclerView.setAdapter(new RssHorizontalAdapter(MainActivity.this, items));
                    startAutoScroll(items.size());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "RSS error: " + error, Toast.LENGTH_SHORT).show();
                    // Fallback sample content if RSS fails
                    java.util.List<RssItem> sample = new java.util.ArrayList<>();
                    sample.add(new RssItem("Aurorus Hoodie", "Premium hoodie with logo", null, ""));
                    sample.add(new RssItem("Sticker Pack", "Vinyl stickers", null, ""));
                    sample.add(new RssItem("Community Meetup", "Join the next meetup", null, ""));
                    feedRecyclerView.setAdapter(new RssHorizontalAdapter(MainActivity.this, sample));
                    startAutoScroll(sample.size());
                });
            }
        });
    }

    private void startAutoScroll(int itemCount) {
        if (itemCount <= 1) return;
        // Cancel any previous runnable
        autoScrollHandler.removeCallbacksAndMessages(null);
        autoScrollPosition = 0;
        autoScrollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RecyclerView.Adapter<?> adapter = feedRecyclerView.getAdapter();
                if (adapter == null || adapter.getItemCount() == 0) return;
                autoScrollPosition = (autoScrollPosition + 1) % adapter.getItemCount();
                feedRecyclerView.smoothScrollToPosition(autoScrollPosition);
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_INTERVAL_MS);
            }
        }, AUTO_SCROLL_INTERVAL_MS);
    }

    private void fetchAndBindProductsCarousel() {
        android.util.Log.d("MainActivity", "Setting up products carousel");
        // Horizontal layout manager for products
        LinearLayoutManager horizontalLm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        productsRecyclerView.setLayoutManager(horizontalLm);
        productsRecyclerView.post(() -> productsRecyclerView.smoothScrollToPosition(0));

        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("TBL_PRODUCTS");
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                java.util.List<ProductItem> products = new java.util.ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ProductItem item = snapshot.getValue(ProductItem.class);
                    if (item == null) item = new ProductItem();
                    if (item.id == null) item.id = snapshot.getKey();
                    
                    // Map various possible field names
                    if (item.name == null) item.name = snapshot.child("name").getValue(String.class);
                    if (item.name == null) item.name = snapshot.child("productName").getValue(String.class);
                    if (item.name == null) item.name = snapshot.child("title").getValue(String.class);
                    
                    if (item.description == null) item.description = snapshot.child("description").getValue(String.class);
                    if (item.description == null) item.description = snapshot.child("desc").getValue(String.class);
                    
                    if (item.imageBase64 == null) {
                        String b64 = snapshot.child("imageBase64").getValue(String.class);
                        if (b64 == null) b64 = snapshot.child("image").getValue(String.class);
                        item.imageBase64 = b64;
                    }
                    
                    if (item.imageUrl == null) item.imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    
                    if (item.price == 0) {
                        Double priceD = snapshot.child("price").getValue(Double.class);
                        if (priceD == null) {
                            Long priceL = snapshot.child("price").getValue(Long.class);
                            if (priceL != null) priceD = priceL.doubleValue();
                        }
                        if (priceD == null) {
                            String priceS = snapshot.child("price").getValue(String.class);
                            try { if (priceS != null) priceD = Double.parseDouble(priceS); } catch (Exception ignored) {}
                        }
                        if (priceD != null) item.price = priceD;
                    }
                    
                    if (item.stocks == null) {
                        Long stocksL = coerceLong(snapshot.child("stocks"));
                        if (stocksL == null) stocksL = coerceLong(snapshot.child("stock"));
                        if (stocksL == null) stocksL = coerceLong(snapshot.child("quantity"));
                        if (stocksL == null) stocksL = coerceLong(snapshot.child("qty"));
                        item.stocks = stocksL;
                    }
                    
                    products.add(item);
                }
                
                if (products.isEmpty()) {
                    // Show sample products if none found
                    ProductItem sample1 = new ProductItem();
                    sample1.id = "sample_hoodie";
                    sample1.name = "Aurorus Hoodie";
                    sample1.description = "Premium hoodie with logo";
                    sample1.price = 1200.0;
                    sample1.stocks = 10L;
                    
                    ProductItem sample2 = new ProductItem();
                    sample2.id = "sample_stickers";
                    sample2.name = "Sticker Pack";
                    sample2.description = "Vinyl stickers";
                    sample2.price = 150.0;
                    sample2.stocks = 25L;
                    
                    products.add(sample1);
                    products.add(sample2);
                }
                
                android.util.Log.d("MainActivity", "Setting products adapter with " + products.size() + " products");
                productsRecyclerView.setAdapter(new ProductsHorizontalAdapter(MainActivity.this, products));
                startProductsAutoScroll(products.size());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Show sample products on error
                java.util.List<ProductItem> sample = new java.util.ArrayList<>();
                ProductItem sample1 = new ProductItem();
                sample1.id = "sample_hoodie";
                sample1.name = "Aurorus Hoodie";
                sample1.description = "Premium hoodie with logo";
                sample1.price = 1200.0;
                sample1.stocks = 10L;
                
                ProductItem sample2 = new ProductItem();
                sample2.id = "sample_stickers";
                sample2.name = "Sticker Pack";
                sample2.description = "Vinyl stickers";
                sample2.price = 150.0;
                sample2.stocks = 25L;
                
                sample.add(sample1);
                sample.add(sample2);
                
                android.util.Log.d("MainActivity", "Setting products adapter with sample data: " + sample.size() + " products");
                productsRecyclerView.setAdapter(new ProductsHorizontalAdapter(MainActivity.this, sample));
                startProductsAutoScroll(sample.size());
            }
        });
    }

    private void startProductsAutoScroll(int itemCount) {
        android.util.Log.d("MainActivity", "Starting products auto-scroll with " + itemCount + " items");
        if (itemCount <= 1) {
            android.util.Log.d("MainActivity", "Not enough items for auto-scroll");
            return;
        }
        // Cancel any previous runnable
        productsAutoScrollHandler.removeCallbacksAndMessages(null);
        productsAutoScrollPosition = 0;
        productsAutoScrollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RecyclerView.Adapter<?> adapter = productsRecyclerView.getAdapter();
                if (adapter == null || adapter.getItemCount() == 0) {
                    android.util.Log.d("MainActivity", "No adapter or items for products auto-scroll");
                    return;
                }
                productsAutoScrollPosition = (productsAutoScrollPosition + 1) % adapter.getItemCount();
                android.util.Log.d("MainActivity", "Products auto-scroll to position: " + productsAutoScrollPosition);
                productsRecyclerView.smoothScrollToPosition(productsAutoScrollPosition);
                productsAutoScrollHandler.postDelayed(this, AUTO_SCROLL_INTERVAL_MS);
            }
        }, AUTO_SCROLL_INTERVAL_MS);
    }

    private Long coerceLong(DataSnapshot snapshot) {
        if (snapshot == null) return null;
        Long value = snapshot.getValue(Long.class);
        if (value != null) return value;
        Integer intValue = snapshot.getValue(Integer.class);
        if (intValue != null) return intValue.longValue();
        String strValue = snapshot.getValue(String.class);
        if (strValue != null) {
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void updateUserInfo() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // Fetch user data from Firebase Database
            usersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String userName = dataSnapshot.child("name").getValue(String.class);
                        if (userName != null && !userName.isEmpty()) {
                            welcomeTextView.setText("Welcome, " + userName);
                        } else {
                            welcomeTextView.setText("Welcome, User");
                        }
                    } else {
                        welcomeTextView.setText("Welcome, User");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    welcomeTextView.setText("Welcome, User");
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for broadcasts from our service
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyHostApduService.ACTION_NFC_STATUS);
        registerReceiver(nfcReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        
        // Check NFC status when returning to the app
        checkNfcStatus();
        // Resume auto-scroll if adapters present
        RecyclerView.Adapter<?> feedAdapter = feedRecyclerView.getAdapter();
        if (feedAdapter != null) {
            startAutoScroll(feedAdapter.getItemCount());
        }
        
        RecyclerView.Adapter<?> productsAdapter = productsRecyclerView.getAdapter();
        if (productsAdapter != null) {
            startProductsAutoScroll(productsAdapter.getItemCount());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        unregisterReceiver(nfcReceiver);
        // Stop auto-scroll while not visible
        if (autoScrollHandler != null) {
            autoScrollHandler.removeCallbacksAndMessages(null);
        }
        if (productsAutoScrollHandler != null) {
            productsAutoScrollHandler.removeCallbacksAndMessages(null);
        }
    }
    
    private void startCheckIn() {
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            // Start HCE service
            getPackageManager().setComponentEnabledSetting(
                hceService,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            );
            
            // Launch check-in activity
            Intent intent = new Intent(this, CheckInActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "NFC must be enabled first", Toast.LENGTH_SHORT).show();
            showNfcSettings();
        }
    }
    
    private void stopHceService() {
        getPackageManager().setComponentEnabledSetting(
            hceService,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        );
    }
    
    private void initNfc() {
        // Get NFC adapter instance
        NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        nfcAdapter = nfcManager.getDefaultAdapter();
        
        if (nfcAdapter == null) {
            // Device does not support NFC
            Toast.makeText(this, "This device doesn't support NFC", Toast.LENGTH_LONG).show();
        } else if (!nfcAdapter.isEnabled()) {
            // NFC is not enabled
            showNfcSettings();
        }
    }
    
    private void showNfcSettings() {
        Toast.makeText(this, "Please enable NFC", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
    }
    
    private void checkNfcStatus() {
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            showNfcSettings();
        }
    }

    private void openBattlePass() {
        Intent intent = new Intent(this, CheckInHistoryActivity.class);
        startActivity(intent);
                    }

    private void openStoreCredits() {
        Intent intent = new Intent(this, StoreCreditsMainActivity.class);
        startActivity(intent);
    }
    
    private void openCatalog() {
        Intent intent = new Intent(this, CatalogActivity.class);
        startActivity(intent);
    }
    
    private void openProducts() {
        Intent intent = new Intent(this, ProductsActivity.class);
        startActivity(intent);
    }
    
    private void openAccount() {
        Intent intent = new Intent(this, AccountActivity.class);
        startActivity(intent);
    }
    
    private void handleLogout() {
        firebaseAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
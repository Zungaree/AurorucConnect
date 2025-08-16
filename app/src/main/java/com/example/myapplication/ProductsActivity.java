package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private ProductsAdapter adapter;
    private final List<ProductItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        recyclerView = findViewById(R.id.productsRecyclerView);
        progressBar = findViewById(R.id.productsProgressBar);
        emptyTextView = findViewById(R.id.productsEmptyText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductsAdapter(items);
        recyclerView.setAdapter(adapter);

        loadProducts();
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("TBL_PRODUCTS");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ProductItem item = child.getValue(ProductItem.class);
                        if (item == null) item = new ProductItem();
                        if (item.id == null || item.id.isEmpty()) {
                            String key = child.getKey();
                            item.id = key;
                        }
                        // Fallbacks for common field names
                        if (item.name == null || item.name.isEmpty()) {
                            String n = child.child("name").getValue(String.class);
                            if (n == null || n.isEmpty()) n = child.child("productName").getValue(String.class);
                            if (n == null || n.isEmpty()) n = child.child("productname").getValue(String.class);
                            if (n == null || n.isEmpty()) n = child.child("title").getValue(String.class);
                            item.name = n;
                        }
                        if (item.description == null || item.description.isEmpty()) {
                            String d = child.child("description").getValue(String.class);
                            if (d == null || d.isEmpty()) d = child.child("desc").getValue(String.class);
                            if (d == null || d.isEmpty()) d = child.child("productDescription").getValue(String.class);
                            if (d == null || d.isEmpty()) d = child.child("details").getValue(String.class);
                            item.description = d;
                        }
                        if (item.imageUrl == null) item.imageUrl = child.child("imageUrl").getValue(String.class);
                        if (item.imageBase64 == null) {
                            String b64 = child.child("imageBase64").getValue(String.class);
                            if (b64 == null) b64 = child.child("image").getValue(String.class);
                            item.imageBase64 = b64;
                        }
                        // price and stocks mapping with fallbacks
                        if (item.price == 0) {
                            Double priceD = child.child("price").getValue(Double.class);
                            if (priceD == null) {
                                Long priceL = child.child("price").getValue(Long.class);
                                if (priceL != null) priceD = priceL.doubleValue();
                            }
                            if (priceD == null) {
                                String priceS = child.child("price").getValue(String.class);
                                try { if (priceS != null) priceD = Double.parseDouble(priceS); } catch (Exception ignored) {}
                            }
                            if (priceD != null) item.price = priceD;
                        }
                        if (item.stocks == null) {
                            Long stocksL = null;
                            // Primary key
                            stocksL = coerceLong(child.child("stocks"));
                            if (stocksL == null) stocksL = coerceLong(child.child("stock"));
                            if (stocksL == null) stocksL = coerceLong(child.child("Stock"));
                            if (stocksL == null) stocksL = coerceLong(child.child("quantity"));
                            if (stocksL == null) stocksL = coerceLong(child.child("qty"));
                            if (stocksL == null) stocksL = coerceLong(child.child("available"));
                            if (stocksL == null) stocksL = coerceLong(child.child("availableStock"));
                            if (stocksL == null) stocksL = coerceLong(child.child("availableQty"));
                            if (stocksL == null) stocksL = coerceLong(child.child("stockCount"));
                            if (stocksL == null) stocksL = coerceLong(child.child("count"));
                            if (stocksL == null) stocksL = coerceLong(child.child("inventory"));
                            if (stocksL == null) stocksL = coerceLong(child.child("remaining"));
                            if (stocksL == null) stocksL = coerceLong(child.child("balance"));
                            if (stocksL == null) stocksL = coerceLong(child.child("units"));
                            if (stocksL == null) stocksL = coerceLong(child.child("pieces"));
                            if (stocksL == null) stocksL = coerceLong(child.child("pcs"));
                            item.stocks = stocksL;
                        }
                        items.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
                emptyTextView.setText("Failed to load products: " + error.getMessage());
            }
        });
    }

    private Long coerceLong(DataSnapshot node) {
        if (node == null) return null;
        try {
            Long v = node.getValue(Long.class);
            if (v != null) return v;
        } catch (Exception ignored) {}
        try {
            Integer i = node.getValue(Integer.class);
            if (i != null) return i.longValue();
        } catch (Exception ignored) {}
        try {
            String s = node.getValue(String.class);
            if (s != null) return Long.parseLong(s.trim());
        } catch (Exception ignored) {}
        try {
            Double d = node.getValue(Double.class);
            if (d != null) return d.longValue();
        } catch (Exception ignored) {}
        return null;
    }
}



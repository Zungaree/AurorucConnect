package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView titleView;
    private TextView priceView;
    private TextView stocksView;
    private TextView descriptionView;
    private Button payNowButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        imageView = findViewById(R.id.detailImage);
        titleView = findViewById(R.id.detailTitle);
        priceView = findViewById(R.id.detailPrice);
        stocksView = findViewById(R.id.detailStocks);
        descriptionView = findViewById(R.id.detailDescription);
        payNowButton = findViewById(R.id.payNowButton);

        String id = getIntent().getStringExtra("id");
        if (TextUtils.isEmpty(id)) {
            Toast.makeText(this, "Invalid product", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Handle sample products
        if (id.startsWith("sample_")) {
            handleSampleProduct(id);
            return;
        }

        // Fetch product by id
        com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("TBL_PRODUCTS").child(id);
        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ProductDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                ProductItem item = snapshot.getValue(ProductItem.class);
                if (item == null) item = new ProductItem();
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

                bindUi(item);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Toast.makeText(ProductDetailActivity.this, "Failed to load product", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        payNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleView.getText() != null ? titleView.getText().toString() : "";
                Intent i = new Intent(ProductDetailActivity.this, ProductPaymentActivity.class);
                i.putExtra("productName", title);
                startActivity(i);
            }
        });
    }

    private void bindUi(ProductItem item) {
        titleView.setText(item.name != null ? item.name : "");
        priceView.setText(item.price > 0 ? String.format("₱%.2f", item.price) : "");
        stocksView.setText(item.stocks != null ? ("Stocks: " + item.stocks) : "");
        descriptionView.setText(!TextUtils.isEmpty(item.description) ? item.description : "");

        boolean loaded = false;
        if (!TextUtils.isEmpty(item.imageBase64)) {
            try {
                String b64 = item.imageBase64;
                int commaIdx = b64.indexOf(',');
                if (b64.startsWith("data:image") && commaIdx > -1) {
                    b64 = b64.substring(commaIdx + 1);
                }
                byte[] decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    imageView.setImageBitmap(bmp);
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }
        if (!loaded && !TextUtils.isEmpty(item.imageUrl)) {
            Glide.with(this).load(item.imageUrl).into(imageView);
            loaded = true;
        }
        if (!loaded) {
            imageView.setImageResource(R.drawable.platinum);
        }
    }

    private Long coerceLong(com.google.firebase.database.DataSnapshot node) {
        if (node == null) return null;
        try { Long v = node.getValue(Long.class); if (v != null) return v; } catch (Exception ignored) {}
        try { Integer i = node.getValue(Integer.class); if (i != null) return i.longValue(); } catch (Exception ignored) {}
        try { String s = node.getValue(String.class); if (s != null) return Long.parseLong(s.trim()); } catch (Exception ignored) {}
        try { Double d = node.getValue(Double.class); if (d != null) return d.longValue(); } catch (Exception ignored) {}
        return null;
    }

    private void handleSampleProduct(String id) {
        ProductItem item = new ProductItem();
        item.id = id;
        
        if ("sample_hoodie".equals(id)) {
            item.name = "Aurorus Hoodie";
            item.description = "Premium hoodie with logo";
            item.price = 1200.0;
            item.stocks = 10L;
        } else if ("sample_stickers".equals(id)) {
            item.name = "Sticker Pack";
            item.description = "Vinyl stickers";
            item.price = 150.0;
            item.stocks = 25L;
        }
        
        bindUi(item);
    }
}



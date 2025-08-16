package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductsHorizontalAdapter extends RecyclerView.Adapter<ProductsHorizontalAdapter.ViewHolder> {
    private List<ProductItem> products;
    private Context context;

    public ProductsHorizontalAdapter(Context context, List<ProductItem> products) {
        this.context = context;
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_horizontal_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem product = products.get(position);
        
        holder.titleTextView.setText(product.name != null ? product.name : "Product");
        
        String description = "";
        if (product.description != null && !product.description.isEmpty()) {
            description = product.description;
        }
        if (product.price > 0) {
            description += (description.isEmpty() ? "" : " • ") + String.format("₱%.2f", product.price);
        }
        if (product.stocks != null) {
            description += (description.isEmpty() ? "" : " • ") + "Stock: " + product.stocks;
        }
        holder.descriptionTextView.setText(description);

        // Load image
        boolean loaded = false;
        if (product.imageBase64 != null && !product.imageBase64.isEmpty()) {
            try {
                String b64 = product.imageBase64;
                int commaIdx = b64.indexOf(',');
                if (b64.startsWith("data:image") && commaIdx > -1) {
                    b64 = b64.substring(commaIdx + 1);
                }
                byte[] imageBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }
        
        if (!loaded && product.imageUrl != null && !product.imageUrl.isEmpty()) {
            Glide.with(context).load(product.imageUrl).into(holder.imageView);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("id", product.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView descriptionTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.cardImage);
            titleTextView = itemView.findViewById(R.id.cardTitle);
            descriptionTextView = itemView.findViewById(R.id.cardDescription);
        }
    }
}

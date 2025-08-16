package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ProductViewHolder> {
    private final List<ProductItem> items;

    public ProductsAdapter(List<ProductItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductItem item = items.get(position);
        String title = item.name != null ? item.name : "";
        holder.title.setText(title);
        String priceText = item.price > 0 ? String.format("₱%.2f", item.price) : "";
        String stocksText = item.stocks != null ? ("Stocks: " + item.stocks) : "";
        String desc = item.description != null ? item.description : "";
        String meta = (priceText.isEmpty() ? "" : priceText) + (stocksText.isEmpty() ? "" : (priceText.isEmpty() ? "" : "  •  ") + stocksText);
        holder.description.setText(meta.isEmpty() ? desc : meta + (desc.isEmpty() ? "" : "\n" + desc));
        if (item.imageBase64 != null && !item.imageBase64.isEmpty()) {
            try {
                String b64 = item.imageBase64;
                int commaIdx = b64.indexOf(',');
                if (b64.startsWith("data:image") && commaIdx > -1) {
                    b64 = b64.substring(commaIdx + 1);
                }
                byte[] decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    holder.image.setImageBitmap(bmp);
                } else if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                    Glide.with(holder.image.getContext()).load(item.imageUrl).into(holder.image);
                } else {
                    holder.image.setImageResource(R.drawable.platinum);
                }
            } catch (Exception e) {
                if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                    Glide.with(holder.image.getContext()).load(item.imageUrl).into(holder.image);
                } else {
                    holder.image.setImageResource(R.drawable.platinum);
                }
            }
        } else if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.image.getContext()).load(item.imageUrl).into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.platinum);
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Context ctx = holder.itemView.getContext();
            android.content.Intent intent = new android.content.Intent(ctx, ProductDetailActivity.class);
            intent.putExtra("id", item.id);
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView description;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.productImage);
            title = itemView.findViewById(R.id.productTitle);
            description = itemView.findViewById(R.id.productDescription);
        }
    }
}



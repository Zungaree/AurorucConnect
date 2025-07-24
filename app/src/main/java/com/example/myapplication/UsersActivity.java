package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class UsersActivity extends AppCompatActivity {
    private RecyclerView usersRecyclerView;
    private ProgressBar progressBar;
    private UserAdapter adapter;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Initialize views
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        // Set up RecyclerView
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter();
        usersRecyclerView.setAdapter(adapter);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://db-aurorus-default-rtdb.asia-southeast1.firebasedatabase.app");
        usersRef = database.getReference("users");

        // Load users
        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserData> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String userData = "Data: " + userSnapshot.getValue().toString();
                    users.add(new UserData(userId, userData));
                }
                adapter.setUsers(users);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UsersActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Data class for User
    private static class UserData {
        String id;
        String data;

        UserData(String id, String data) {
            this.id = id;
            this.data = data;
        }
    }

    // RecyclerView Adapter
    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<UserData> users = new ArrayList<>();

        void setUsers(List<UserData> users) {
            this.users = users;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserData user = users.get(position);
            holder.userIdTextView.setText(user.id);
            holder.userDataTextView.setText(user.data);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            TextView userIdTextView;
            TextView userDataTextView;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                userIdTextView = itemView.findViewById(R.id.userIdTextView);
                userDataTextView = itemView.findViewById(R.id.userDataTextView);
            }
        }
    }
} 
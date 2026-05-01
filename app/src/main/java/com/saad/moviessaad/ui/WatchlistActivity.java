package com.saad.moviessaad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.saad.moviessaad.R;
import com.saad.moviessaad.adapter.WatchlistAdapter;
import com.saad.moviessaad.data.SupabaseService;
import com.saad.moviessaad.model.WatchlistItem;

public class WatchlistActivity extends AppCompatActivity {

    private WatchlistAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchlist);

        SessionManager sessionManager = new SessionManager();
        userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_watchlist);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        adapter = new WatchlistAdapter(new WatchlistAdapter.OnWatchlistActionListener() {
            @Override
            public void onMovieClick(WatchlistItem item) {
                Intent intent = new Intent(WatchlistActivity.this, MovieDetailActivity.class);
                intent.putExtra("movie_id", item.getMovieId());
                intent.putExtra("movie_title", item.getTitle());
                intent.putExtra("movie_poster", item.getPosterPath());
                intent.putExtra("movie_rating", item.getRating());
                startActivity(intent);
            }

            @Override
            public void onRemoveClick(WatchlistItem item) {
                removeFromWatchlist(item.getMovieId());
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                WatchlistItem item = adapter.getItemAt(viewHolder.getBindingAdapterPosition());
                removeFromWatchlist(item.getMovieId());
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        observeWatchlist();
    }

    private void observeWatchlist() {
        progressBar.setVisibility(View.VISIBLE);
        SupabaseService.INSTANCE.loadWatchlist(userId, new SupabaseService.WatchlistCallback() {
            @Override
            public void onSuccess(java.util.List<WatchlistItem> items) {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(items);
                emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                showSnack("Unable to load watchlist", R.color.colorError);
            }
        });
    }

    private void removeFromWatchlist(int movieId) {
        SupabaseService.INSTANCE.removeWatchlistItem(userId, movieId, new SupabaseService.ActionCallback() {
            @Override
            public void onSuccess() {
                showSnack("Removed from Watchlist", R.color.colorMuted);
                observeWatchlist();
            }

            @Override
            public void onError(String message) {
                showSnack("Unable to remove from Watchlist", R.color.colorError);
            }
        });
    }

    private void showSnack(String message, int colorRes) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setActionTextColor(getColor(colorRes));
        snackbar.show();
    }

}

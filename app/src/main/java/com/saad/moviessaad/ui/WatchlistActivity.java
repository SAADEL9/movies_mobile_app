package com.saad.moviessaad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
        SystemBarInsets.applyToRootWithoutBottom(findViewById(android.R.id.content));

        SessionManager sessionManager = new SessionManager();
        userId = sessionManager.getCurrentUserId();
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_watchlist);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        setupBottomNavigation();

        adapter = new WatchlistAdapter(new WatchlistAdapter.OnWatchlistActionListener() {
            @Override
            public void onMovieClick(WatchlistItem item) {
                Intent intent = new Intent(WatchlistActivity.this, MovieDetailActivity.class);
                intent.putExtra("movie_id", item.getMovieId());
                intent.putExtra("movie_title", item.getTitle());
                intent.putExtra("movie_poster", item.getPosterPath());
                intent.putExtra("movie_rating", item.getRating());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            @Override
            public void onRemoveClick(WatchlistItem item) {
                int position = findItemPosition(item);
                if (position >= 0) {
                    removeWithUndo(position, item);
                }
            }
        });

        // 2-column grid layout
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // Swipe to delete with undo
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                WatchlistItem item = adapter.getItemAt(position);
                removeWithUndo(position, item);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        observeWatchlist();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_watchlist);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_watchlist) {
                return true;
            } else if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_watchlist);
        }
    }

    private int findItemPosition(WatchlistItem item) {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getItemAt(i).getId() == item.getId()) {
                return i;
            }
        }
        return -1;
    }

    private void removeWithUndo(int position, WatchlistItem item) {
        adapter.removeItem(position);
        updateEmptyState();

        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                getString(R.string.removed_from_watchlist), Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setActionTextColor(getColor(R.color.colorPrimary));
        snackbar.setAction(getString(R.string.undo), v -> {
            adapter.insertItem(position, item);
            updateEmptyState();
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    // User did not undo — actually delete from Supabase
                    SupabaseService.INSTANCE.removeWatchlistItem(userId, item.getMovieId(), new SupabaseService.ActionCallback() {
                        @Override
                        public void onSuccess() { }

                        @Override
                        public void onError(String message) {
                            // Re-insert on failure
                            adapter.insertItem(position, item);
                            updateEmptyState();
                            showSnack("Unable to remove from Watchlist", R.color.colorError);
                        }
                    });
                }
            }
        });
        snackbar.show();
    }

    private void updateEmptyState() {
        emptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void observeWatchlist() {
        progressBar.setVisibility(View.VISIBLE);
        SupabaseService.INSTANCE.loadWatchlist(userId, new SupabaseService.WatchlistCallback() {
            @Override
            public void onSuccess(java.util.List<WatchlistItem> items) {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(items);
                updateEmptyState();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                showSnack("Unable to load watchlist", R.color.colorError);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}

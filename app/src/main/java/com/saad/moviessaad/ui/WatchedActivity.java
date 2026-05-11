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
import com.saad.moviessaad.adapter.WatchedAdapter;
import com.saad.moviessaad.api.SupabaseApiClient;
import com.saad.moviessaad.api.SupabaseDbService;
import com.saad.moviessaad.model.WatchedItem;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchedActivity extends AppCompatActivity {

    private WatchedAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private String userId;
    private String userToken;
    private SupabaseDbService supabaseDbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watched);
        SystemBarInsets.applyToRootWithoutBottom(findViewById(android.R.id.content));

        SessionManager sessionManager = new SessionManager();
        userId = sessionManager.getCurrentUserId();
        userToken = sessionManager.getToken();
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        supabaseDbService = SupabaseApiClient.getClient().create(SupabaseDbService.class);

        RecyclerView recyclerView = findViewById(R.id.recycler_watched);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        setupBottomNavigation();

        adapter = new WatchedAdapter(item -> {
            Intent intent = new Intent(WatchedActivity.this, MovieDetailActivity.class);
            intent.putExtra("movie_id", item.getMovieId());
            intent.putExtra("movie_title", item.getTitle());
            intent.putExtra("movie_poster", item.getPosterPath());
            intent.putExtra("movie_rating", (double) item.getRating());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                WatchedItem item = adapter.getItemAt(position);
                removeWithUndo(position, item);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        fetchWatchedMovies();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        // We don't have a dedicated nav item for watched in the provided menu,
        // but we'll handle it if it's added.
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_watchlist) {
                startActivity(new Intent(this, WatchlistActivity.class));
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

    private void fetchWatchedMovies() {
        progressBar.setVisibility(View.VISIBLE);
        supabaseDbService.getWatched("Bearer " + userToken, SupabaseApiClient.SUPABASE_KEY, "eq." + userId, "watched_at.desc").enqueue(new Callback<List<WatchedItem>>() {
            @Override
            public void onResponse(Call<List<WatchedItem>> call, Response<List<WatchedItem>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                    updateEmptyState();
                } else {
                    showSnack("Failed to load watched movies", R.color.colorError);
                }
            }

            @Override
            public void onFailure(Call<List<WatchedItem>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showSnack("Connection error", R.color.colorError);
            }
        });
    }

    private void removeWithUndo(int position, WatchedItem item) {
        adapter.removeItem(position);
        updateEmptyState();

        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                "Removed from Watched", Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setActionTextColor(getColor(R.color.colorPrimary));
        snackbar.setAction("UNDO", v -> {
            adapter.insertItem(position, item);
            updateEmptyState();
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    supabaseDbService.removeWatched("Bearer " + userToken, SupabaseApiClient.SUPABASE_KEY, userId, "eq." + item.getMovieId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {}
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {}
                    });
                }
            }
        });
        snackbar.show();
    }

    private void updateEmptyState() {
        emptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
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

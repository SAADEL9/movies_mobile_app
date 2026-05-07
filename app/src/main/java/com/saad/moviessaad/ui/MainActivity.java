package com.saad.moviessaad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.saad.moviessaad.R;
import com.saad.moviessaad.adapter.MovieAdapter;
import com.saad.moviessaad.api.ApiClient;
import com.saad.moviessaad.api.ApiConstants;
import com.saad.moviessaad.api.ApiService;
import com.saad.moviessaad.data.SupabaseService;
import com.saad.moviessaad.model.Movie;
import com.saad.moviessaad.model.MovieResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité principale affichant la liste des films
 */
public class MainActivity extends AppCompatActivity implements MovieAdapter.OnMovieClickListener {

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private ProgressBar progressBar;
    private ApiService apiService;
    private List<Movie> movieList = new ArrayList<>();
    private MaterialToolbar toolbar;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager();
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = sessionManager.getCurrentUserId();
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        SearchView searchView = findViewById(R.id.search_view);
        preventSearchKeyboardOnLaunch(searchView);
        loadGreeting();

        // 2-column grid layout
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new MovieAdapter(movieList, this);
        recyclerView.setAdapter(adapter);

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
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

        apiService = ApiClient.getClient().create(ApiService.class);
        fetchNowPlayingMovies();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.isEmpty()) {
                    searchMovies(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && !newText.isEmpty()) {
                    searchMovies(newText);
                } else {
                    fetchNowPlayingMovies();
                }
                return true;
            }
        });

        findViewById(R.id.fab_chat).setOnClickListener(v -> {
            Intent intent = new Intent(this, AvatarChatActivity.class);
            intent.putExtra("mode", "general");
            startActivity(intent);
        });
    }

    private void preventSearchKeyboardOnLaunch(SearchView searchView) {
        searchView.clearFocus();
        recyclerView.requestFocus();
        searchView.post(() -> {
            searchView.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            }
        });
    }

    private void loadGreeting() {
        if (toolbar == null || userId == null) return;
        SupabaseService.INSTANCE.loadDisplayName(userId, new SupabaseService.DisplayNameCallback() {
            @Override
            public void onSuccess(String displayName) {
                String name = displayName == null || displayName.trim().isEmpty()
                        ? "Alex"
                        : displayName.trim();
                toolbar.setTitle("Hi, " + name);
            }

            @Override
            public void onError(String message) {
                toolbar.setTitle("Hi, Alex");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset bottom nav selection when returning
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void fetchNowPlayingMovies() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getNowPlayingMovies(ApiConstants.API_KEY).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    movieList = response.body().getResults();
                    adapter.setMovieList(movieList);
                } else {
                    showError("Error while fetching movies");
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void searchMovies(String query) {
        apiService.searchMovies(ApiConstants.API_KEY, query).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMovieList(response.body().getResults());
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
            }
        });

        findViewById(R.id.fab_chat).setOnClickListener(v -> {
            Intent intent = new Intent(this, AvatarChatActivity.class);
            intent.putExtra("mode", "general");
            startActivity(intent);
        });
    }

    @Override
    public void onMovieClick(Movie movie) {
        Intent intent = new Intent(this, MovieDetailActivity.class);
        intent.putExtra("movie_id", movie.getId());
        intent.putExtra("movie_title", movie.getTitle());
        intent.putExtra("movie_poster", movie.getPosterPath());
        intent.putExtra("movie_rating", movie.getVoteAverage());
        intent.putExtra("movie_overview", movie.getOverview());
        intent.putExtra("movie_release", movie.getReleaseDate());
        intent.putExtra("movie_backdrop", movie.getBackdropPath());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showError(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setActionTextColor(getColor(R.color.colorError));
        snackbar.show();
    }
}

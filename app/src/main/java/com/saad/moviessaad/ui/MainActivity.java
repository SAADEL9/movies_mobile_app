package com.saad.moviessaad.ui;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
import java.util.Locale;
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
    private ChipGroup categoryChipGroup;
    private ChipGroup topRatedChipGroup;
    private String userId;
    private String userType;

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
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        userType = prefs.getString("user_type", "adult");

        setContentView(R.layout.activity_main);
        SystemBarInsets.applyToRootWithoutBottom(findViewById(android.R.id.content));

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        categoryChipGroup = findViewById(R.id.category_chip_group);
        topRatedChipGroup = findViewById(R.id.top_rated_chip_group);
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
        setupDiscoveryControls();
        fetchNowPlayingMovies();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    runSearch(query.trim());
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && !newText.trim().isEmpty()) {
                    runSearch(newText.trim());
                } else {
                    clearDiscoveryChecks();
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

    private void setupDiscoveryControls() {
        setCategoryChip(R.id.chip_action, 28);
        setCategoryChip(R.id.chip_romance, 10749);
        setCategoryChip(R.id.chip_comedy, 35);
        setCategoryChip(R.id.chip_horror, 27);
        setCategoryChip(R.id.chip_scifi, 878);
        setCategoryChip(R.id.chip_animation, 16);

        Chip topMovies = findViewById(R.id.chip_top_movies);
        Chip topSeries = findViewById(R.id.chip_top_series);
        topMovies.setOnClickListener(v -> {
            categoryChipGroup.clearCheck();
            fetchTopRatedMovies();
        });
        topSeries.setOnClickListener(v -> {
            categoryChipGroup.clearCheck();
            fetchTopRatedSeries();
        });
    }

    private void setCategoryChip(int chipId, int genreId) {
        Chip chip = findViewById(chipId);
        chip.setOnClickListener(v -> {
            topRatedChipGroup.clearCheck();
            fetchMoviesByGenre(chip.getText().toString(), genreId);
        });
    }

    private void clearDiscoveryChecks() {
        if (categoryChipGroup != null) categoryChipGroup.clearCheck();
        if (topRatedChipGroup != null) topRatedChipGroup.clearCheck();
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
        if ("kid".equals(userType)) {
            // Fetch animation/family movies for kids
            fetchMoviesByGenre("Kids", 10751); 
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        apiService.getNowPlayingMovies(ApiConstants.API_KEY).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    movieList = normalizedResults(response.body().getResults(), "movie");
                    adapter.setMovieList(movieList);
                    toolbar.setSubtitle("Let's find something great to watch.");
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

    private void fetchMoviesByGenre(String genreName, int genreId) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.discoverMoviesByGenre(ApiConstants.API_KEY, genreId, "popularity.desc").enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    movieList = normalizedResults(response.body().getResults(), "movie");
                    adapter.setMovieList(movieList);
                    toolbar.setSubtitle(genreName + " movies");
                } else {
                    showError("Error while fetching " + genreName + " movies");
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void fetchTopRatedMovies() {
        fetchRankedList(apiService.getTopRatedMovies(ApiConstants.API_KEY), "movie", "Best movies by rating");
    }

    private void fetchTopRatedSeries() {
        fetchRankedList(apiService.getTopRatedSeries(ApiConstants.API_KEY), "tv", "Best series by rating");
    }

    private void fetchRankedList(Call<MovieResponse> call, String mediaType, String subtitle) {
        progressBar.setVisibility(View.VISIBLE);
        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    movieList = normalizedResults(response.body().getResults(), mediaType);
                    adapter.setMovieList(movieList);
                    toolbar.setSubtitle(subtitle);
                } else {
                    showError("Error while fetching top rated titles");
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void runSearch(String query) {
        int genreId = genreIdForQuery(query);
        if (genreId != -1) {
            topRatedChipGroup.clearCheck();
            fetchMoviesByGenre(displayGenreName(query), genreId);
            return;
        }
        clearDiscoveryChecks();
        searchMovies(query);
    }

    private void searchMovies(String query) {
        apiService.searchMovies(ApiConstants.API_KEY, query).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMovieList(normalizedResults(response.body().getResults(), "movie"));
                    toolbar.setSubtitle("Search results for \"" + query + "\"");
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
            }
        });
    }

    private List<Movie> normalizedResults(List<Movie> results, String mediaType) {
        if (results == null) return new ArrayList<>();
        for (Movie movie : results) {
            movie.setMediaType(mediaType);
        }
        return results;
    }

    private int genreIdForQuery(String query) {
        String normalized = query.toLowerCase(Locale.ROOT).replace("-", "").replace(" ", "");
        switch (normalized) {
            case "action":
                return 28;
            case "romance":
            case "romantic":
                return 10749;
            case "comedy":
                return 35;
            case "horror":
                return 27;
            case "scifi":
            case "sciencefiction":
                return 878;
            case "animation":
            case "animated":
                return 16;
            default:
                return -1;
        }
    }

    private String displayGenreName(String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return "Category";
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1).toLowerCase(Locale.ROOT);
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
        intent.putExtra("media_type", movie.getMediaType());
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

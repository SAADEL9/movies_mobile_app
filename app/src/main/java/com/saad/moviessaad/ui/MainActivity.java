package com.saad.moviessaad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager();
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        SearchView searchView = findViewById(R.id.search_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MovieAdapter(movieList, this);
        recyclerView.setAdapter(adapter);

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
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_watchlist) {
            startActivity(new Intent(this, WatchlistActivity.class));
            return true;
        }
        if (id == R.id.action_logout) {
            SupabaseService.INSTANCE.signOut(new SupabaseService.ActionCallback() {
                @Override
                public void onSuccess() {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                @Override
                public void onError(String message) {
                    showError("Unable to logout");
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    }

    private void showError(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setActionTextColor(getColor(R.color.colorError));
        snackbar.show();
    }
}

package com.saad.moviessaad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.saad.moviessaad.R;
import com.saad.moviessaad.adapter.MovieAdapter;
import com.saad.moviessaad.api.ApiClient;
import com.saad.moviessaad.api.ApiConstants;
import com.saad.moviessaad.api.ApiService;
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
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        SearchView searchView = findViewById(R.id.search_view);

        // Configuration du RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MovieAdapter(movieList, this);
        recyclerView.setAdapter(adapter);

        // Initialisation de l'API Service
        apiService = ApiClient.getClient().create(ApiService.class);

        // Chargement initial des films
        fetchNowPlayingMovies();

        // Configuration de la recherche
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

    /**
     * Appelle l'API pour récupérer les films actuellement au cinéma
     */
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
                    showError("Erreur lors de la récupération des films");
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("Erreur réseau : " + t.getMessage());
            }
        });
    }

    /**
     * Recherche des films via l'API
     * @param query Terme recherché
     */
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
                // Erreur silencieuse pour la recherche continue
            }
        });
    }

    /**
     * Gère le clic sur un film pour ouvrir les détails
     * @param movie Le film cliqué
     */
    @Override
    public void onMovieClick(Movie movie) {
        Intent intent = new Intent(this, MovieDetailActivity.class);
        intent.putExtra("movie_data", movie);
        startActivity(intent);
    }

    /**
     * Affiche un message d'erreur via Toast
     * @param message Message à afficher
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

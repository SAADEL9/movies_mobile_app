package com.saad.moviessaad.api;

import com.saad.moviessaad.model.CreditsResponse;
import com.saad.moviessaad.model.MovieResponse;
import com.saad.moviessaad.model.VideoResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Interface définissant les points de terminaison de l'API TMDB
 */
public interface ApiService {

    /**
     * Récupère la liste des films "Now Playing"
     * @param apiKey Clé API TMDB
     * @return Appel Retrofit vers MovieResponse
     */
    @GET("movie/now_playing")
    Call<MovieResponse> getNowPlayingMovies(@Query("api_key") String apiKey);

    @GET("discover/movie")
    Call<MovieResponse> discoverMoviesByGenre(
            @Query("api_key") String apiKey,
            @Query("with_genres") int genreId,
            @Query("sort_by") String sortBy);

    @GET("movie/top_rated")
    Call<MovieResponse> getTopRatedMovies(@Query("api_key") String apiKey);

    @GET("tv/top_rated")
    Call<MovieResponse> getTopRatedSeries(@Query("api_key") String apiKey);

    /**
     * Recherche des films par nom
     * @param apiKey Clé API TMDB
     * @param query Terme de recherche
     * @param includeAdult Exclure ou inclure le contenu adulte
     * @return Appel Retrofit vers MovieResponse
     */
    @GET("search/movie")
    Call<MovieResponse> searchMovies(
            @Query("api_key") String apiKey,
            @Query("query") String query,
            @Query("include_adult") boolean includeAdult);

    /**
     * Découverte de films avec filtres dynamiques
     */
    @GET("discover/movie")
    Call<MovieResponse> discoverMovies(
            @Query("api_key") String apiKey,
            @QueryMap java.util.Map<String, String> options);

    /**
     * Récupère les vidéos (bandes-annonces) d'un film spécifique
     * @param id Identifiant du film
     * @param apiKey Clé API TMDB
     * @return Appel Retrofit vers VideoResponse
     */
    @GET("movie/{movie_id}/videos")
    Call<VideoResponse> getMovieVideos(@Path("movie_id") int id, @Query("api_key") String apiKey);

    @GET("tv/{series_id}/videos")
    Call<VideoResponse> getSeriesVideos(@Path("series_id") int id, @Query("api_key") String apiKey);

    @GET("movie/{movie_id}/credits")
    Call<CreditsResponse> getCredits(@Path("movie_id") int id, @Query("api_key") String apiKey);
}

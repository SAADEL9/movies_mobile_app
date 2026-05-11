package com.saad.moviessaad.api;

import com.saad.moviessaad.model.UserRating;
import com.saad.moviessaad.model.WatchedItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseDbService {

    // Watched Movies Endpoints
    @POST("rest/v1/watched")
    Call<Void> addWatched(
            @Header("Authorization") String auth,
            @Header("apikey") String apikey,
            @Header("Prefer") String prefer,
            @Body WatchedItem item);

    @GET("rest/v1/watched")
    Call<List<WatchedItem>> getWatched(
            @Header("Authorization") String auth,
            @Header("apikey") String apikey,
            @Query("user_id") String userId,
            @Query("order") String order);

    @GET("rest/v1/watched")
    Call<List<WatchedItem>> checkWatched(
            @Header("Authorization") String auth,
            @Header("apikey") String apikey,
            @Query("user_id") String userId,
            @Query("movie_id") String movieId);

    @DELETE("rest/v1/watched")
    Call<Void> removeWatched(
            @Header("Authorization") String auth,
            @Header("apikey") String apikey,
            @Query("user_id") String userId,
            @Query("movie_id") String movieId);

    // Ratings Endpoints
    @POST("rest/v1/ratings")
    Call<Void> submitRating(
            @Header("Authorization") String auth,
            @Header("apikey") String apikey,
            @Header("Prefer") String prefer,
            @Body UserRating rating);

    @GET("rest/v1/ratings")
    Call<List<UserRating>> getUserRating(
            @Header("Authorization") String auth,
            @Header("apikey") String apikey,
            @Query("user_id") String userId,
            @Query("movie_id") String movieId);

    @DELETE("rest/v1/ratings")
    Call<Void> deleteRating(
            @Header("Authorization") String auth,
            @Header("apikey") String apikey,
            @Query("user_id") String userId,
            @Query("movie_id") String movieId);
}

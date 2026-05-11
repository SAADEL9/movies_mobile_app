package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;

public class WatchedItem {
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("movie_id")
    private int movieId;
    private String title;
    @SerializedName("poster_path")
    private String posterPath;
    private float rating;
    @SerializedName("watched_at")
    private String watchedAt;

    public WatchedItem(String userId, int movieId, String title, String posterPath, float rating) {
        this.userId = userId;
        this.movieId = movieId;
        this.title = title;
        this.posterPath = posterPath;
        this.rating = rating;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public String getPosterPath() { return posterPath; }
    public float getRating() { return rating; }
    public String getWatchedAt() { return watchedAt; }
}

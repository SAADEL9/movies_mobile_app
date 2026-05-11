package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;

public class UserRating {
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("movie_id")
    private int movieId;
    private float rating;
    @SerializedName("rated_at")
    private String ratedAt;

    public UserRating(String userId, int movieId, float rating) {
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getMovieId() { return movieId; }
    public float getRating() { return rating; }
    public String getRatedAt() { return ratedAt; }
}

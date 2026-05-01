package com.saad.moviessaad.model;

public class WatchlistItem {
    private int movieId;
    private String title;
    private String posterPath;
    private double rating;
    private String addedAt;

    public WatchlistItem() {
    }

    public WatchlistItem(int movieId, String title, String posterPath, double rating, String addedAt) {
        this.movieId = movieId;
        this.title = title;
        this.posterPath = posterPath;
        this.rating = rating;
        this.addedAt = addedAt;
    }

    public int getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public double getRating() {
        return rating;
    }

    public String getAddedAt() {
        return addedAt;
    }
}

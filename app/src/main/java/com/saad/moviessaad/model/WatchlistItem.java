package com.saad.moviessaad.model;

public class WatchlistItem {
    private long id;
    private int movieId;
    private String title;
    private String posterPath;
    private double rating;
    private String addedAt;
    private boolean watched;

    public WatchlistItem() {
    }

    public WatchlistItem(long id, int movieId, String title, String posterPath, double rating, String addedAt) {
        this(id, movieId, title, posterPath, rating, addedAt, false);
    }

    public WatchlistItem(long id, int movieId, String title, String posterPath, double rating, String addedAt, boolean watched) {
        this.id = id;
        this.movieId = movieId;
        this.title = title;
        this.posterPath = posterPath;
        this.rating = rating;
        this.addedAt = addedAt;
        this.watched = watched;
    }

    public long getId() {
        return id;
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

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }
}

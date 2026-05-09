package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Modèle de données pour un Film
 */
public class Movie implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("name")
    private String name;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("overview")
    private String overview;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("first_air_date")
    private String firstAirDate;

    @SerializedName("vote_average")
    private double voteAverage;

    @SerializedName("media_type")
    private String mediaType;

    public Movie(int id, String title, String posterPath, String backdropPath, String overview, String releaseDate, double voteAverage) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
    }

    public int getId() { return id; }
    public String getTitle() { return title != null ? title : name; }
    public String getPosterPath() { return posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public String getOverview() { return overview; }
    public String getReleaseDate() { return releaseDate != null ? releaseDate : firstAirDate; }
    public double getVoteAverage() { return voteAverage; }
    public String getMediaType() { return mediaType != null ? mediaType : "movie"; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
}

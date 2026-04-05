package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Réponse de l'API contenant la liste des films
 */
public class MovieResponse {
    @SerializedName("results")
    private List<Movie> results;

    public List<Movie> getResults() {
        return results;
    }
}

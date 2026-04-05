package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Réponse de l'API contenant la liste des vidéos
 */
public class VideoResponse {
    @SerializedName("results")
    private List<Video> results;

    public List<Video> getResults() {
        return results;
    }
}

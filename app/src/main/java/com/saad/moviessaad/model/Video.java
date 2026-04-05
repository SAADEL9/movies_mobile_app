package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle de données pour une vidéo (Bande-annonce)
 */
public class Video {
    @SerializedName("id")
    private String id;

    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("site")
    private String site;

    @SerializedName("type")
    private String type;

    public String getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public String getSite() { return site; }
    public String getType() { return type; }
}

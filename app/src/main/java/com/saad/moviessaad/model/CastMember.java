package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;

public class CastMember {
    private int id;
    private String name;
    private String character;
    @SerializedName("profile_path")
    private String profilePath;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCharacter() { return character; }
    public String getProfilePath() { return profilePath; }
}

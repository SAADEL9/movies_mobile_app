package com.saad.moviessaad.model;

import com.google.gson.annotations.SerializedName;

public class CrewMember {
    private int id;
    private String name;
    private String job;
    @SerializedName("profile_path")
    private String profilePath;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getJob() { return job; }
    public String getProfilePath() { return profilePath; }
}

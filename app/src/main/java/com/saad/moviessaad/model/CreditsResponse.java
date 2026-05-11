package com.saad.moviessaad.model;

import java.util.List;

public class CreditsResponse {
    private List<CastMember> cast;
    private List<CrewMember> crew;

    public List<CastMember> getCast() { return cast; }
    public List<CrewMember> getCrew() { return crew; }
}

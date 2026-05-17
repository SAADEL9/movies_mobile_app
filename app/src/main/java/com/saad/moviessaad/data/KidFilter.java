package com.saad.moviessaad.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

public class KidFilter {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_USER_TYPE = "user_type";

    public static boolean isKid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userType = prefs.getString(KEY_USER_TYPE, "adult");
        return "kid".equals(userType);
    }

    /**
     * Builds a map of query parameters for TMDB's discover/movie endpoint
     * when the user is a kid.
     */
    public static Map<String, String> getKidQueryParams() {
        Map<String, String> params = new HashMap<>();
        params.put("certification_country", "US");
        params.put("certification.lte", "PG");
        params.put("with_genres", "16,10751,12,35"); // Animation, Family, Adventure, Comedy
        params.put("include_adult", "false");
        params.put("vote_count.gte", "10");
        return params;
    }
}

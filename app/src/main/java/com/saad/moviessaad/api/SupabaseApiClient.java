package com.saad.moviessaad.api;

import com.saad.moviessaad.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseApiClient {
    private static Retrofit retrofit = null;
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(SUPABASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

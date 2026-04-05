package com.saad.moviessaad.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton pour l'instance Retrofit
 */
public class ApiClient {
    private static Retrofit retrofit = null;

    /**
     * Retourne l'instance unique de Retrofit
     * @return Retrofit instance
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConstants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

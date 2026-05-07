package com.saad.moviessaad.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    Call<OpenRouterResponse> chat(
            @Header("Authorization") String authorization,
            @Header("HTTP-Referer") String referer,
            @Header("X-Title") String title,
            @Body OpenRouterRequest request
    );
}

package com.saad.moviessaad.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OllamaApiService {
    @POST("api/chat")
    Call<OllamaResponse> chat(@Body OllamaRequest request);
}

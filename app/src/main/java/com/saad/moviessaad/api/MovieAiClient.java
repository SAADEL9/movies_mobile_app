package com.saad.moviessaad.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieAiClient {
    public interface ReplyCallback {
        void onSuccess(OllamaMessage message, String provider);
        void onError(String message);
    }

    public static void chat(List<OllamaMessage> history, ReplyCallback callback) {
        OllamaRequest request = new OllamaRequest(OllamaConfig.MODEL, history, false);
        OllamaClient.getApiService().chat(request).enqueue(new Callback<OllamaResponse>() {
            @Override
            public void onResponse(Call<OllamaResponse> call, Response<OllamaResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getMessage() != null) {
                    callback.onSuccess(response.body().getMessage(), "Ollama");
                    return;
                }
                tryOpenRouter(history, callback, "Ollama returned an empty response.");
            }

            @Override
            public void onFailure(Call<OllamaResponse> call, Throwable t) {
                tryOpenRouter(history, callback, "Ollama error: " + t.getMessage());
            }
        });
    }

    private static void tryOpenRouter(List<OllamaMessage> history, ReplyCallback callback, String ollamaError) {
        String apiKey = OllamaConfig.OPENROUTER_API_KEY == null ? "" : OllamaConfig.OPENROUTER_API_KEY.trim();
        if (apiKey.isEmpty()) {
            callback.onError(ollamaError + " Add your OpenRouter API key in OllamaConfig.OPENROUTER_API_KEY to use the fallback.");
            return;
        }

        OpenRouterRequest request = new OpenRouterRequest(OllamaConfig.OPENROUTER_MODEL, history, false);
        OpenRouterClient.getApiService()
                .chat("Bearer " + apiKey, "https://cinebot.local", "CineBot", request)
                .enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getChoices() != null
                                && !response.body().getChoices().isEmpty()
                                && response.body().getChoices().get(0).getMessage() != null) {
                            callback.onSuccess(response.body().getChoices().get(0).getMessage(), "OpenRouter");
                        } else {
                            callback.onError("OpenRouter did not return a usable response.");
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        callback.onError("OpenRouter error: " + t.getMessage());
                    }
                });
    }
}

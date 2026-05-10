package com.saad.moviessaad.api;

import com.saad.moviessaad.BuildConfig;

public class OllamaConfig {
    // Replace with your local machine's IP address where Ollama is running
    // If using Android Emulator, 10.0.2.2 points to host machine's localhost
    public static final String BASE_URL = BuildConfig.OLLAMA_BASE_URL;
    public static final String MODEL = "llama3.2";

    public static final String OPENROUTER_BASE_URL = BuildConfig.OPENROUTER_BASE_URL;
    public static final String OPENROUTER_MODEL = "openrouter/free";
    public static final String OPENROUTER_API_KEY = BuildConfig.OPENROUTER_API_KEY;
}

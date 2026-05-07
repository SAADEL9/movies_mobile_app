package com.saad.moviessaad.api;

import java.util.List;

public class OpenRouterRequest {
    private String model;
    private List<OllamaMessage> messages;
    private boolean stream;

    public OpenRouterRequest(String model, List<OllamaMessage> messages, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
    }

    public String getModel() { return model; }
    public List<OllamaMessage> getMessages() { return messages; }
    public boolean isStream() { return stream; }
}

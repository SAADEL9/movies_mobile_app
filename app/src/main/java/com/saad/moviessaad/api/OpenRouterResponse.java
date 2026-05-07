package com.saad.moviessaad.api;

import java.util.List;

public class OpenRouterResponse {
    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public static class Choice {
        private OllamaMessage message;

        public OllamaMessage getMessage() {
            return message;
        }
    }
}

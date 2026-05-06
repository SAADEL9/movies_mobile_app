package com.saad.moviessaad.model;

public class ChatMessage {
    private String content;
    private boolean isUser;
    private long timestamp;
    private boolean isTyping;

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
        this.isTyping = false;
    }

    public static ChatMessage typingIndicator() {
        ChatMessage msg = new ChatMessage("", false);
        msg.isTyping = true;
        return msg;
    }

    public String getContent() { return content; }
    public boolean isUser() { return isUser; }
    public long getTimestamp() { return timestamp; }
    public boolean isTyping() { return isTyping; }
}

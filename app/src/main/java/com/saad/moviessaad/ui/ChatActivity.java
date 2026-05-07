package com.saad.moviessaad.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.saad.moviessaad.R;
import com.saad.moviessaad.api.MovieAiClient;
import com.saad.moviessaad.api.OllamaMessage;
import com.saad.moviessaad.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> chatMessages;
    private List<OllamaMessage> apiHistory;
    private EditText etMessage;
    private ChipGroup starterChips;
    private View starterChipsScroll;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_chat);
        etMessage = findViewById(R.id.et_message);
        starterChips = findViewById(R.id.starter_chips);
        starterChipsScroll = findViewById(R.id.starter_chips_scroll);
        
        chatMessages = new ArrayList<>();
        apiHistory = new ArrayList<>();
        adapter = new ChatAdapter(chatMessages);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "general";

        setupMode();

        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage(etMessage.getText().toString()));
    }

    private void setupMode() {
        String systemPrompt;
        String[] suggestions;

        if ("movie".equals(mode)) {
            String title = getIntent().getStringExtra("movie_title");
            String overview = getIntent().getStringExtra("movie_overview");
            String year = getIntent().getStringExtra("movie_year");
            String rating = getIntent().getStringExtra("movie_rating");

            getSupportActionBar().setTitle("Movie Chat");
            getSupportActionBar().setSubtitle(title);

            systemPrompt = String.format("You are a movie expert assistant. The user is currently viewing the movie '%s' (%s), rated %s/10. Overview: %s. Answer questions about this movie, recommend similar movies, discuss themes, cast, and trivia.",
                    title, year, rating, overview);
            
            suggestions = new String[]{"Who directed this?", "Similar movies?", "Tell me about the cast"};
        } else {
            getSupportActionBar().setTitle("Movie Assistant");
            systemPrompt = "You are a movie expert. Help the user discover, discuss, and learn about any movie, actor, or director.";
            suggestions = new String[]{"🎬 Recommend a thriller", "⭐ Best movies of 2024", "🎭 Movies like Inception"};
        }

        apiHistory.add(new OllamaMessage("system", systemPrompt));

        for (String suggestion : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(suggestion);
            chip.setChipBackgroundColorResource(R.color.colorSurface);
            chip.setTextColor(getResources().getColor(R.color.colorGold));
            chip.setChipStrokeColorResource(R.color.colorPrimary);
            chip.setChipStrokeWidth(2f);
            chip.setOnClickListener(v -> sendMessage(suggestion));
            starterChips.addView(chip);
        }
    }

    private void sendMessage(String text) {
        if (text.trim().isEmpty()) return;

        etMessage.setText("");
        starterChipsScroll.setVisibility(View.GONE);

        chatMessages.add(new ChatMessage(text, true));
        apiHistory.add(new OllamaMessage("user", text));
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        // Show typing indicator
        chatMessages.add(ChatMessage.typingIndicator());
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        MovieAiClient.chat(apiHistory, new MovieAiClient.ReplyCallback() {
            @Override
            public void onSuccess(OllamaMessage botMsg, String provider) {
                removeTypingIndicator();
                apiHistory.add(botMsg);
                chatMessages.add(new ChatMessage(botMsg.getContent(), false));
                adapter.notifyItemInserted(chatMessages.size() - 1);
                recyclerView.scrollToPosition(chatMessages.size() - 1);
            }

            @Override
            public void onError(String message) {
                removeTypingIndicator();
                chatMessages.add(new ChatMessage(message, false));
                adapter.notifyItemInserted(chatMessages.size() - 1);
                recyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        });
    }

    private void removeTypingIndicator() {
        int lastIndex = chatMessages.size() - 1;
        if (lastIndex >= 0 && chatMessages.get(lastIndex).isTyping()) {
            chatMessages.remove(lastIndex);
            adapter.notifyItemRemoved(lastIndex);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_clear) {
            clearChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearChat() {
        chatMessages.clear();
        OllamaMessage systemMsg = apiHistory.get(0);
        apiHistory.clear();
        apiHistory.add(systemMsg);
        adapter.notifyDataSetChanged();
        starterChipsScroll.setVisibility(View.VISIBLE);
    }
}

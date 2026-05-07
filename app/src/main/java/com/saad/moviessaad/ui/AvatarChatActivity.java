package com.saad.moviessaad.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.saad.moviessaad.R;
import com.saad.moviessaad.api.OllamaApiService;
import com.saad.moviessaad.api.OllamaClient;
import com.saad.moviessaad.api.OllamaConfig;
import com.saad.moviessaad.api.OllamaMessage;
import com.saad.moviessaad.api.OllamaRequest;
import com.saad.moviessaad.api.OllamaResponse;
import com.saad.moviessaad.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import io.github.sceneview.SceneView;
import com.google.android.filament.gltfio.FilamentInstance;
import io.github.sceneview.node.ModelNode;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvatarChatActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_RECORD_AUDIO = 4401;
    private static final String UTTERANCE_ID = "cinebot_utterance";

    private enum AvatarState {
        IDLE,
        WAVING,
        LISTENING,
        TALKING
    }

    private SceneView sceneView;
    private View avatarStage;
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private FrameLayout sceneContainer;
    private View listeningRing;
    private TextView subtitleView;
    private EditText messageInput;
    private ChipGroup starterChips;
    private View starterChipsScroll;
    private TabLayout chatTabs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<OllamaMessage> conversationHistory = new ArrayList<>();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private OllamaApiService apiService;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private ObjectAnimator listeningAnimator;
    private ModelNode currentModelNode;
    private AvatarState state = AvatarState.IDLE;
    private String mode = "general";
    private String introText;
    private boolean ttsReady;
    private String pendingSpeechText;
    private boolean waitingForOllama;
    private boolean avatar3dEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_chat);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("CineBot");
        }

        sceneContainer = findViewById(R.id.scene_container);
        avatarStage = findViewById(R.id.avatar_stage);
        recyclerView = findViewById(R.id.recycler_chat);
        listeningRing = findViewById(R.id.listening_ring);
        subtitleView = findViewById(R.id.tv_subtitle);
        messageInput = findViewById(R.id.et_message);
        starterChips = findViewById(R.id.starter_chips);
        starterChipsScroll = findViewById(R.id.starter_chips_scroll);
        chatTabs = findViewById(R.id.chat_tabs);
        apiService = OllamaClient.getApiService();
        if (canUseTextToSpeech()) {
            textToSpeech = new TextToSpeech(this, this);
        }

        setupMode();
        setupChatList();
        setupTabs();
        setupSceneView();
        setupStarterChips();
        setupSpeechRecognizer();
        setupInput();
        messageInput.clearFocus();
        avatarStage.requestFocus();
        startOpeningFlow();
    }

    private void setupChatList() {
        adapter = new ChatAdapter(chatMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        chatTabs.addTab(chatTabs.newTab().setText("Chat"));
        chatTabs.addTab(chatTabs.newTab().setText("3D Avatar"));
        TabLayout.Tab avatarTab = chatTabs.getTabAt(1);
        if (avatarTab != null) {
            avatarTab.select();
        }
        showAvatarTab();

        chatTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showChatTab();
                } else {
                    showAvatarTab();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showChatTab() {
        recyclerView.setVisibility(View.VISIBLE);
        avatarStage.setVisibility(View.GONE);
        subtitleView.setVisibility(View.GONE);
    }

    private void showAvatarTab() {
        recyclerView.setVisibility(View.GONE);
        avatarStage.setVisibility(View.VISIBLE);
        subtitleView.setVisibility(View.VISIBLE);
    }

    private void setupSceneView() {
        if (!canUse3dAvatar()) {
            avatar3dEnabled = false;
            addAvatarFallback();
            return;
        }

        try {
            sceneView = new SceneView(this);
            sceneContainer.addView(sceneView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        } catch (Throwable throwable) {
            sceneView = null;
            subtitleView.setText("CineBot is ready, but the 3D avatar could not start on this device.");
        }
    }

    private boolean canUse3dAvatar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }

        return !isLikelyEmulator();
    }

    private boolean canUseTextToSpeech() {
        return !isLikelyEmulator();
    }

    private boolean isLikelyEmulator() {
        String deviceInfo = (Build.FINGERPRINT + " " + Build.MODEL + " " + Build.MANUFACTURER + " "
                + Build.BRAND + " " + Build.DEVICE + " " + Build.PRODUCT).toLowerCase(Locale.US);
        return deviceInfo.contains("generic")
                || deviceInfo.contains("emulator")
                || deviceInfo.contains("sdk_gphone")
                || deviceInfo.contains("sdk_phone")
                || deviceInfo.contains("goldfish")
                || deviceInfo.contains("ranchu");
    }

    private void addAvatarFallback() {
        FrameLayout fallbackLayout = new FrameLayout(this);

        ImageView avatarImage = new ImageView(this);
        avatarImage.setImageResource(R.drawable.avatar);
        avatarImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        fallbackLayout.addView(avatarImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        TextView label = new TextView(this);
        label.setText("CineBot");
        label.setGravity(Gravity.CENTER);
        label.setTextColor(ContextCompat.getColor(this, R.color.colorGold));
        label.setTextSize(28f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setBackgroundColor(ContextCompat.getColor(this, R.color.colorScrim));
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        fallbackLayout.addView(label, labelParams);

        sceneContainer.addView(fallbackLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void setupMode() {
        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");
        if (mode == null) mode = "general";

        if ("movie".equals(mode)) {
            String title = safeValue(intent.getStringExtra("movie_title"), "this movie");
            String overview = safeValue(intent.getStringExtra("movie_overview"), "No overview available.");
            String year = safeValue(intent.getStringExtra("movie_year"), "N/A");
            String rating = safeValue(intent.getStringExtra("movie_rating"), "0");
            introText = "Hey! Want to know more about " + title + "? Ask me anything!";
            conversationHistory.add(new OllamaMessage("system",
                    "You are CineBot. The user is viewing '" + title + "' (" + year + "), rated "
                            + rating + "/10. Overview: " + overview
                            + ". Keep ALL responses under 3 sentences. Be enthusiastic."));
        } else {
            introText = "Hey! I am CineBot, your personal movie assistant. Ask me anything about movies!";
            conversationHistory.add(new OllamaMessage("system",
                    "You are CineBot, a friendly movie expert inside a movies app. "
                            + "Keep ALL responses under 3 sentences. Be fun and conversational."));
        }
        subtitleView.setText(introText);
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void setupStarterChips() {
        String[] suggestions = "movie".equals(mode)
                ? new String[]{"What is it about?", "Similar movies", "Fun trivia"}
                : new String[]{"Recommend a thriller", "Best recent movies", "Movies like Inception"};

        for (String suggestion : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(suggestion);
            chip.setChipBackgroundColorResource(R.color.colorSurface);
            chip.setTextColor(ContextCompat.getColor(this, R.color.colorGold));
            chip.setChipStrokeColorResource(R.color.colorPrimary);
            chip.setChipStrokeWidth(2f);
            chip.setOnClickListener(v -> sendMessage(suggestion));
            starterChips.addView(chip);
        }
    }

    private void setupInput() {
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage(messageInput.getText().toString()));
        findViewById(R.id.btn_mic).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startListening();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                stopListening();
                return true;
            }
            return true;
        });
    }

    private void setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int eventType, Bundle params) {}
            @Override public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onError(int error) {
                if (state == AvatarState.LISTENING) {
                    setAvatarState(AvatarState.IDLE);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sendMessage(matches.get(0));
                } else {
                    setAvatarState(AvatarState.IDLE);
                }
            }
        });
    }

    private void startOpeningFlow() {
        loadAvatarModel("avatar.glb");
        handler.postDelayed(() -> setAvatarState(AvatarState.WAVING), 500);
        handler.postDelayed(() -> setAvatarState(AvatarState.IDLE), 3000);
        handler.postDelayed(() -> speak(introText), 3100);
    }

    private void setAvatarState(AvatarState newState) {
        if (state == newState) return;
        state = newState;
        if (newState == AvatarState.IDLE) {
            stopListeningPulse();
            loadAvatarModel("avatar.glb");
        } else if (newState == AvatarState.WAVING) {
            stopListeningPulse();
            loadAvatarModel("waving.glb");
        } else if (newState == AvatarState.LISTENING) {
            startListeningPulse();
        } else if (newState == AvatarState.TALKING) {
            stopListeningPulse();
            loadAvatarModel("talking.glb");
        }
    }

    private void loadAvatarModel(String assetName) {
        if (!avatar3dEnabled || sceneView == null) return;
        try {
            if (currentModelNode != null) {
                sceneView.removeChildNode(currentModelNode);
            }
            FilamentInstance modelInstance = sceneView.getModelLoader()
                    .createModelInstance(assetName, resourceFileName -> null);
            currentModelNode = new ModelNode(modelInstance, true, 1.8f, null);
            sceneView.addChildNode(currentModelNode);
        } catch (Throwable throwable) {
            subtitleView.setText("CineBot could not load " + assetName + ".");
        }
    }

    private void startListening() {
        if (waitingForOllama) return;
        if (speechRecognizer == null) {
            Toast.makeText(this, "Speech recognition is unavailable on this device.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        setAvatarState(AvatarState.LISTENING);
        subtitleView.setText("Listening...");
        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        if (speechRecognizer != null && state == AvatarState.LISTENING) {
            speechRecognizer.stopListening();
        }
    }

    private void sendMessage(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || waitingForOllama) return;

        messageInput.setText("");
        starterChipsScroll.setVisibility(View.GONE);
        subtitleView.setText(trimmed);
        chatMessages.add(new ChatMessage(trimmed, true));
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        conversationHistory.add(new OllamaMessage("user", trimmed));
        waitingForOllama = true;
        setAvatarState(AvatarState.IDLE);

        chatMessages.add(ChatMessage.typingIndicator());
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        OllamaRequest request = new OllamaRequest(OllamaConfig.MODEL, conversationHistory, false);
        apiService.chat(request).enqueue(new Callback<OllamaResponse>() {
            @Override
            public void onResponse(Call<OllamaResponse> call, Response<OllamaResponse> response) {
                waitingForOllama = false;
                removeTypingIndicator();
                if (response.isSuccessful() && response.body() != null && response.body().getMessage() != null) {
                    OllamaMessage botMessage = response.body().getMessage();
                    conversationHistory.add(botMessage);
                    String answer = safeValue(botMessage.getContent(), "I received an empty response from Ollama.");
                    chatMessages.add(new ChatMessage(answer, false));
                    adapter.notifyItemInserted(chatMessages.size() - 1);
                    recyclerView.scrollToPosition(chatMessages.size() - 1);
                    subtitleView.setText(answer);
                    speak(answer);
                } else {
                    showBotError("Sorry, I cannot reach Ollama right now. Make sure Ollama is running on your computer and the model is installed.");
                }
            }

            @Override
            public void onFailure(Call<OllamaResponse> call, Throwable t) {
                waitingForOllama = false;
                removeTypingIndicator();
                showBotError("Error: " + t.getMessage());
            }
        });
    }

    private void showBotError(String message) {
        chatMessages.add(new ChatMessage(message, false));
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
        subtitleView.setText(message);
        setAvatarState(AvatarState.IDLE);
    }

    private void removeTypingIndicator() {
        int lastIndex = chatMessages.size() - 1;
        if (lastIndex >= 0 && chatMessages.get(lastIndex).isTyping()) {
            chatMessages.remove(lastIndex);
            adapter.notifyItemRemoved(lastIndex);
        }
    }

    private void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (textToSpeech == null) return;
        if (!ttsReady) {
            pendingSpeechText = text;
            return;
        }
        setAvatarState(AvatarState.TALKING);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            textToSpeech.setLanguage(Locale.US);
            textToSpeech.setSpeechRate(0.9f);
            textToSpeech.setPitch(1.0f);
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {
                    runOnUiThread(() -> setAvatarState(AvatarState.TALKING));
                }

                @Override public void onDone(String utteranceId) {
                    runOnUiThread(() -> setAvatarState(AvatarState.IDLE));
                }

                @Override public void onError(String utteranceId) {
                    runOnUiThread(() -> setAvatarState(AvatarState.IDLE));
                }
            });
            if (pendingSpeechText != null) {
                String text = pendingSpeechText;
                pendingSpeechText = null;
                speak(text);
            }
        }
    }

    private void startListeningPulse() {
        if (listeningAnimator == null) {
            listeningAnimator = ObjectAnimator.ofFloat(listeningRing, View.ALPHA, 0.2f, 1.0f);
            listeningAnimator.setDuration(700);
            listeningAnimator.setRepeatMode(ValueAnimator.REVERSE);
            listeningAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        }
        listeningRing.setVisibility(View.VISIBLE);
        listeningAnimator.start();
    }

    private void stopListeningPulse() {
        if (listeningAnimator != null) {
            listeningAnimator.cancel();
        }
        listeningRing.setAlpha(0f);
        listeningRing.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_clear) {
            clearConversation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearConversation() {
        OllamaMessage systemPrompt = conversationHistory.get(0);
        conversationHistory.clear();
        conversationHistory.add(systemPrompt);
        chatMessages.clear();
        adapter.notifyDataSetChanged();
        starterChipsScroll.setVisibility(View.VISIBLE);
        subtitleView.setText(introText);
        setAvatarState(AvatarState.IDLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

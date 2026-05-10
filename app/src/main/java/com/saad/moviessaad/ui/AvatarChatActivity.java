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
import android.speech.tts.Voice;
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
import com.saad.moviessaad.api.MovieAiClient;
import com.saad.moviessaad.api.OllamaMessage;
import com.saad.moviessaad.model.ChatMessage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import io.github.sceneview.SceneView;
import com.google.android.filament.gltfio.FilamentInstance;
import io.github.sceneview.node.ModelNode;
import dev.romainguy.kotlin.math.Float3;

public class AvatarChatActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_RECORD_AUDIO = 4401;
    private static final String UTTERANCE_ID = "cinebot_utterance";

    // ─── Avatar scale & camera tuning constants ───────────────────────────────
    // Increase AVATAR_SCALE to make the avatar bigger, decrease to make smaller.
    private static final float AVATAR_SCALE       = 0.4f;
    // AVATAR_Y: negative moves avatar downward so the head sits in the center.
    private static final float AVATAR_Y           = 0.0f;
    // CAMERA_Y: positive tilts camera upward (looks at face/chest level).
    private static final float CAMERA_Y           = 0.8f;
    // CAMERA_Z: distance from avatar — increase to zoom out, decrease to zoom in.
    private static final float CAMERA_Z           = 3.5f;
    // ──────────────────────────────────────────────────────────────────────────

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
    private View avatarFallbackView;
    private View listeningRing;
    private TextView subtitleView;
    private EditText messageInput;
    private ChipGroup starterChips;
    private View starterChipsScroll;
    private View conversationPanel;
    private View inputContainer;
    private TabLayout chatTabs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<OllamaMessage> conversationHistory = new ArrayList<>();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
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
        SystemBarInsets.applyToRoot(findViewById(android.R.id.content));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("CineBot");
        }

        sceneContainer    = findViewById(R.id.scene_container);
        avatarStage       = findViewById(R.id.avatar_stage);
        recyclerView      = findViewById(R.id.recycler_chat);
        listeningRing     = findViewById(R.id.listening_ring);
        subtitleView      = findViewById(R.id.tv_subtitle);
        messageInput      = findViewById(R.id.et_message);
        starterChips      = findViewById(R.id.starter_chips);
        starterChipsScroll= findViewById(R.id.starter_chips_scroll);
        conversationPanel = findViewById(R.id.conversation_panel);
        inputContainer    = findViewById(R.id.input_container);
        chatTabs          = findViewById(R.id.chat_tabs);

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

    // ─── Chat list ────────────────────────────────────────────────────────────

    private void setupChatList() {
        adapter = new ChatAdapter(chatMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // ─── Tabs ─────────────────────────────────────────────────────────────────

    private void setupTabs() {
        chatTabs.addTab(chatTabs.newTab().setText("Chat"));
        chatTabs.addTab(chatTabs.newTab().setText("3D Avatar"));

        // Default to avatar tab
        TabLayout.Tab avatarTab = chatTabs.getTabAt(1);
        if (avatarTab != null) avatarTab.select();
        showAvatarTab();

        chatTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) showChatTab();
                else showAvatarTab();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showChatTab() {
        recyclerView.setVisibility(View.VISIBLE);
        avatarStage.setVisibility(View.GONE);
        conversationPanel.setVisibility(View.VISIBLE);
        inputContainer.setVisibility(View.VISIBLE);
        subtitleView.setVisibility(View.GONE);
        starterChipsScroll.setVisibility(chatMessages.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAvatarTab() {
        recyclerView.setVisibility(View.GONE);
        avatarStage.setVisibility(View.VISIBLE);
        conversationPanel.setVisibility(View.VISIBLE);
        inputContainer.setVisibility(View.VISIBLE);
        subtitleView.setVisibility(View.VISIBLE);
        starterChipsScroll.setVisibility(View.GONE);
        messageInput.clearFocus();
        avatarStage.requestFocus();
    }

    // ─── SceneView / 3D avatar ────────────────────────────────────────────────

    private void setupSceneView() {
        addAvatarFallback();

        if (!canUse3dAvatar()) {
            avatar3dEnabled = false;
            avatarFallbackView.setVisibility(View.VISIBLE);
            return;
        }

        try {
            sceneView = new SceneView(this);
            sceneContainer.addView(sceneView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            avatarFallbackView.setVisibility(View.GONE);
            avatar3dEnabled = true;
        } catch (Throwable t) {
            sceneView = null;
            avatar3dEnabled = false;
            avatarFallbackView.setVisibility(View.VISIBLE);
            subtitleView.setText("CineBot is ready, but the 3D avatar could not start on this device.");
        }
    }

    private boolean canUse3dAvatar() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    private boolean canUseTextToSpeech() {
        return true;
    }

    private void addAvatarFallback() {
        if (avatarFallbackView != null) return;

        FrameLayout fallbackLayout = new FrameLayout(this);

        ImageView avatarImage = new ImageView(this);
        avatarImage.setImageResource(R.drawable.avatar);
        avatarImage.setAdjustViewBounds(true);
        avatarImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        avatarImage.setBackgroundColor(ContextCompat.getColor(this, R.color.colorBackground));
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

        avatarFallbackView = fallbackLayout;
        sceneContainer.addView(fallbackLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    // ─── Mode (general vs movie) ──────────────────────────────────────────────

    private void setupMode() {
        Intent intent = getIntent();
        mode = intent.getStringExtra("mode");
        if (mode == null) mode = "general";

        if ("movie".equals(mode)) {
            String title    = safeValue(intent.getStringExtra("movie_title"),    "this movie");
            String overview = safeValue(intent.getStringExtra("movie_overview"), "No overview available.");
            String year     = safeValue(intent.getStringExtra("movie_year"),     "N/A");
            String rating   = safeValue(intent.getStringExtra("movie_rating"),   "0");

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
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    // ─── Starter chips ────────────────────────────────────────────────────────

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

    // ─── Input (send + mic) ───────────────────────────────────────────────────

    private void setupInput() {
        findViewById(R.id.btn_send).setOnClickListener(v ->
                sendMessage(messageInput.getText().toString()));

        findViewById(R.id.btn_mic).setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startListening();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopListening();
                    return true;
            }
            return true;
        });
    }

    // ─── Speech recognizer ────────────────────────────────────────────────────

    private void setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;

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
                if (state == AvatarState.LISTENING) setAvatarState(AvatarState.IDLE);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sendMessage(matches.get(0));
                } else {
                    setAvatarState(AvatarState.IDLE);
                }
            }
        });
    }

    // ─── Opening flow ─────────────────────────────────────────────────────────

    private void startOpeningFlow() {
        // Step 1: load idle avatar immediately
        loadAvatarModel("avatar.glb");

        // Step 2: start waving after 500ms
        handler.postDelayed(() -> {
            setAvatarState(AvatarState.WAVING);
        }, 500);

        // Step 3: go back to idle after waving
        handler.postDelayed(() -> {
            setAvatarState(AvatarState.IDLE);
        }, 3500);

        // Step 4: speak intro after waving finishes
        handler.postDelayed(() -> {
            speak(introText);
        }, 3600);
    }

    // ─── Avatar state machine ─────────────────────────────────────────────────

    private void setAvatarState(AvatarState newState) {
        if (state == newState) return;
        state = newState;

        switch (newState) {
            case IDLE:
                stopListeningPulse();
                loadAvatarModel("avatar.glb");
                break;
            case WAVING:
                stopListeningPulse();
                loadAvatarModel("waving.glb");
                break;
            case LISTENING:
                startListeningPulse();
                // keep current model — just show the ring
                break;
            case TALKING:
                stopListeningPulse();
                loadAvatarModel("talking.glb");
                break;
        }
    }

    // ─── Load GLB model ───────────────────────────────────────────────────────

    private void loadAvatarModel(String assetName) {
        if (!avatar3dEnabled || sceneView == null) return;

        try {
            // Remove previous node cleanly
            if (currentModelNode != null) {
                sceneView.removeChildNode(currentModelNode);
                currentModelNode = null;
            }

            FilamentInstance modelInstance = sceneView.getModelLoader()
                    .createModelInstance(assetName, resourceFileName -> null);

            // ── Position tuning ──────────────────────────────────────────────
            // AVATAR_SCALE : overall size of the avatar
            // AVATAR_Y     : vertical offset (negative = move down so head centers)
            currentModelNode = new ModelNode(
                    modelInstance,
                    true,
                    AVATAR_SCALE,
                    new Float3(0.0f, AVATAR_Y, 0.0f)
            );

            // Rotate 180° so avatar faces the camera
            currentModelNode.setRotation(new Float3(0.0f, 180.0f, 0.0f));

            sceneView.addChildNode(currentModelNode);

            // ── Camera tuning ────────────────────────────────────────────────
            // CAMERA_Y : height of camera (positive = look at chest/face level)
            // CAMERA_Z : distance from avatar (larger = more zoomed out)
            if (sceneView.getCameraNode() != null) {
                sceneView.getCameraNode().setPosition(new Float3(0.0f, CAMERA_Y, CAMERA_Z));
                sceneView.getCameraNode().setRotation(new Float3(-10.0f, 0.0f, 0.0f));
            }

        } catch (Throwable t) {
            // Graceful degradation: show message but keep app running
            runOnUiThread(() ->
                    subtitleView.setText("CineBot could not load the 3D model.")
            );
        }
    }

    // ─── Voice input ──────────────────────────────────────────────────────────

    private void startListening() {
        if (waitingForOllama) return;

        if (speechRecognizer == null) {
            Toast.makeText(this, "Speech recognition is unavailable on this device.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        if (textToSpeech != null) textToSpeech.stop();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
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

    // ─── Send message to Ollama ───────────────────────────────────────────────

    private void sendMessage(String text) {
        String trimmed = (text == null) ? "" : text.trim();
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

        // Show typing indicator
        chatMessages.add(ChatMessage.typingIndicator());
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        MovieAiClient.chat(conversationHistory, new MovieAiClient.ReplyCallback() {
            @Override
            public void onSuccess(OllamaMessage botMessage, String provider) {
                waitingForOllama = false;
                removeTypingIndicator();
                conversationHistory.add(botMessage);
                String answer = safeValue(botMessage.getContent(), "I received an empty response.");
                chatMessages.add(new ChatMessage(answer, false));
                adapter.notifyItemInserted(chatMessages.size() - 1);
                recyclerView.scrollToPosition(chatMessages.size() - 1);
                subtitleView.setText(answer);
                speak(answer);
            }

            @Override
            public void onError(String message) {
                waitingForOllama = false;
                removeTypingIndicator();
                showBotError(message);
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

    // ─── Text To Speech ───────────────────────────────────────────────────────

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
            selectMaleVoice();
            textToSpeech.setSpeechRate(0.9f);
            textToSpeech.setPitch(0.78f);
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    runOnUiThread(() -> setAvatarState(AvatarState.TALKING));
                }
                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> setAvatarState(AvatarState.IDLE));
                }
                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> setAvatarState(AvatarState.IDLE));
                }
            });

            // Speak any text that arrived before TTS was ready
            if (pendingSpeechText != null) {
                String text = pendingSpeechText;
                pendingSpeechText = null;
                speak(text);
            }
        }
    }

    private void selectMaleVoice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || textToSpeech == null) return;

        Set<Voice> voices = textToSpeech.getVoices();
        if (voices == null || voices.isEmpty()) return;

        Voice fallback = null;
        for (Voice voice : voices) {
            Locale locale = voice.getLocale();
            if (locale == null || !Locale.US.getLanguage().equals(locale.getLanguage())) continue;
            if (fallback == null) fallback = voice;

            String name = voice.getName() == null ? "" : voice.getName().toLowerCase(Locale.US);
            Set<String> features = voice.getFeatures() == null
                    ? new HashSet<>()
                    : voice.getFeatures();
            String featureText = features.toString().toLowerCase(Locale.US);
            boolean looksMale = (name.contains("male") && !name.contains("female"))
                    || featureText.contains("male");
            if (looksMale && !voice.isNetworkConnectionRequired()) {
                textToSpeech.setVoice(voice);
                return;
            }
        }

        if (fallback != null) {
            textToSpeech.setVoice(fallback);
        }
    }

    // ─── Listening pulse animation ────────────────────────────────────────────

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
        if (listeningAnimator != null) listeningAnimator.cancel();
        listeningRing.setAlpha(0f);
        listeningRing.setVisibility(View.VISIBLE);
    }

    // ─── Options menu ─────────────────────────────────────────────────────────

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

    // ─── Permissions ──────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

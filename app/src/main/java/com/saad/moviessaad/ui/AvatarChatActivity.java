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
import android.util.Log;
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
import com.google.android.filament.Box;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.saad.moviessaad.R;
import com.saad.moviessaad.api.MovieAiClient;
import com.saad.moviessaad.api.OllamaMessage;
import com.saad.moviessaad.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import io.github.sceneview.SceneView;
import com.google.android.filament.gltfio.FilamentInstance;
import io.github.sceneview.node.ModelNode;
import dev.romainguy.kotlin.math.Float3;

public class AvatarChatActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "AVATAR";
    private static final int REQUEST_RECORD_AUDIO = 4401;
    private static final String UTTERANCE_ID = "cinebot_utterance";

    // ── Tuning constants ─────────────────────────────────────────────────────
    private static final float DESIRED_HEIGHT = 0.75f;
    private static final float CAMERA_DISTANCE_FACTOR = 0.75f;
    private static final float CAMERA_HEIGHT_FACTOR = 0.55f;
    private static final float CAMERA_LOOKAT_FACTOR = 0.70f;
    // Ms to wait before reading bounding box
    private static final int BOUNDS_READ_DELAY_MS = 400;
    private static final float AVATAR_X = 0f;
    private static final float AVATAR_Y = -0.5f;
    private static final float AVATAR_Z = 0f;
    private enum AvatarState {
        IDLE, WAVING, LISTENING, TALKING
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

    // Cached framing from avatar.glb — reused for animation-only GLBs
    // (waving.glb & talking.glb have no mesh so bounding box = zero)
    private float cachedScale = -1f;
    private float cachedYPos  = -999f;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

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

        sceneContainer     = findViewById(R.id.scene_container);
        avatarStage        = findViewById(R.id.avatar_stage);
        recyclerView       = findViewById(R.id.recycler_chat);
        listeningRing      = findViewById(R.id.listening_ring);
        subtitleView       = findViewById(R.id.tv_subtitle);
        messageInput       = findViewById(R.id.et_message);
        starterChips       = findViewById(R.id.starter_chips);
        starterChipsScroll = findViewById(R.id.starter_chips_scroll);
        conversationPanel  = findViewById(R.id.conversation_panel);
        inputContainer     = findViewById(R.id.input_container);
        chatTabs           = findViewById(R.id.chat_tabs);

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
        if (state != AvatarState.LISTENING && state != AvatarState.TALKING) {
            subtitleView.setText(introText);
        }
    }

    // ─── SceneView ────────────────────────────────────────────────────────────

    private void setupSceneView() {
        addAvatarFallback();

        if (!canUse3dAvatar()) {
            avatar3dEnabled = false;
            avatarFallbackView.setVisibility(View.VISIBLE);
            return;
        }

        try {
            sceneView = new SceneView(this);

            // Return true to fully consume ALL touch events so
            // SceneView's internal gesture detector never fires
            // and never resets the camera back to the feet position
            sceneView.setOnTouchListener((v, event) -> true);

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

    // ─── Mode ─────────────────────────────────────────────────────────────────

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

    // ─── Input ────────────────────────────────────────────────────────────────

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
        loadAvatarModel("avatar.glb");

        // Wave after avatar is fully framed (give extra time for framing)
        handler.postDelayed(() -> setAvatarState(AvatarState.WAVING), 1200);

        // Back to idle
        handler.postDelayed(() -> setAvatarState(AvatarState.IDLE), 4500);

        // Speak intro
        handler.postDelayed(() -> speak(introText), 4600);
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
                // Keep current model — just show pulsing ring
                startListeningPulse();
                break;
            case TALKING:
                stopListeningPulse();
                loadAvatarModel("talking.glb");
                break;
        }
    }

    // ─── Load GLB + auto-frame ────────────────────────────────────────────────
    private void loadAvatarModel(String assetName) {
        if (!avatar3dEnabled || sceneView == null) return;

        try {
            FilamentInstance modelInstance = sceneView
                    .getModelLoader()
                    .createModelInstance(assetName, resourceFileName -> null);

            if (modelInstance == null) {
                Log.e(TAG, "ModelInstance null: " + assetName);
                return;
            }

            ModelNode newNode = new ModelNode(
                    modelInstance,
                    true,
                    1.0f,
                    new Float3(0f, 0f, 0f)
            );

            ModelNode oldNode = currentModelNode;

            // For animations: keep exact same size and fixed position
            if (!assetName.equals("avatar.glb") && oldNode != null) {

                Float3 currentScale = oldNode.getWorldScale();
                Float3 currentPosition = oldNode.getWorldPosition();

                // Boost animation scale
                float animationScaleMultiplier = 80.0f;

// talking avatar too big → reduce it
                if (assetName.equals("talking.glb")) {
                    animationScaleMultiplier = 65.0f;
                }

// waving avatar too small → increase it
                if (assetName.equals("waving.glb")) {
                    animationScaleMultiplier = 90.0f;
                }

                newNode.setWorldScale(
                        new Float3(
                                currentScale.getX() * animationScaleMultiplier,
                                currentScale.getY() * animationScaleMultiplier,
                                currentScale.getZ() * animationScaleMultiplier
                        )
                );

                float animationYOffset = 0f;

// talking animation appears too high
                if (assetName.equals("talking.glb")) {
                    animationYOffset = -0.5f;
                }

// waving animation stays normal
                if (assetName.equals("waving.glb")) {
                    animationYOffset = 0f;
                }

                newNode.setWorldPosition(
                        new Float3(
                                currentPosition.getX(),
                                currentPosition.getY() + animationYOffset,
                                currentPosition.getZ()
                        )
                );

                sceneView.addChildNode(newNode);

                sceneView.removeChildNode(oldNode);

                currentModelNode = newNode;

                Log.d(TAG, "Animation scale fixed");

                return;
            }

            sceneView.addChildNode(newNode);

            if (oldNode != null) {
                sceneView.removeChildNode(oldNode);
            }

            currentModelNode = newNode;

            handler.postDelayed(
                    () -> frameAvatar(newNode, assetName, 1),
                    BOUNDS_READ_DELAY_MS
            );

        } catch (Exception e) {
            Log.e(TAG, "loadAvatarModel failed: " + e.getMessage());
        }
    }
    private void frameAvatar(ModelNode nodeRef, String assetName, int attempt) {
        if (nodeRef != currentModelNode || sceneView == null) return;

        try {
            Box boundingBox = nodeRef.getBoundingBox();

            if (boundingBox == null) {
                throw new IllegalStateException("Bounding box null");
            }

            float[] half = boundingBox.getHalfExtent();
            float modelHeight = half[1] * 2f;

            if (modelHeight <= 0.001f) {
                throw new IllegalStateException("Invalid model height");
            }

            float scale = DESIRED_HEIGHT / modelHeight;

            nodeRef.setWorldScale(
                    new Float3(scale, scale, scale)
            );

            // FIXED POSITION
            nodeRef.setWorldPosition(
                    new Float3(
                            AVATAR_X,
                            AVATAR_Y,
                            AVATAR_Z
                    )
            );

            cachedScale = scale;
            cachedYPos = AVATAR_Y;

            positionCamera();

            Log.d(TAG, "Avatar fixed at X=" + AVATAR_X +
                    " Y=" + AVATAR_Y +
                    " Z=" + AVATAR_Z);

        } catch (Exception e) {
            Log.e(TAG, "frameAvatar failed attempt " + attempt);

            if (attempt < 4) {
                handler.postDelayed(
                        () -> frameAvatar(nodeRef, assetName, attempt + 1),
                        300
                );
            } else {
                applyFallback(nodeRef);
            }
        }
    }

    private void positionCamera() {
        if (sceneView == null || sceneView.getCameraNode() == null) return;

        float camY    = DESIRED_HEIGHT * CAMERA_HEIGHT_FACTOR;
        float camZ    = DESIRED_HEIGHT * CAMERA_DISTANCE_FACTOR;
        float lookAtY = DESIRED_HEIGHT * CAMERA_LOOKAT_FACTOR;

        sceneView.getCameraNode().setWorldPosition(new Float3(0f, camY, camZ));

        // Positive X rotation = look up in right-handed OpenGL (Rx(θ)·[0,0,-1] = [0,sinθ,-cosθ]).
        // lookAtY is above camY (face level > camera height) so deltaY > 0 → positive pitch → look up.
        // The original code negated deltaY, which aimed thea camera at the floor instead of the face.
        float deltaY   = lookAtY - camY;
        float pitchDeg = (float) Math.toDegrees(Math.atan2(deltaY, camZ));
        sceneView.getCameraNode().setRotation(new Float3(pitchDeg, 0f, 0f));

        Log.d(TAG, "Camera Y=" + camY + " Z=" + camZ + " lookAtY=" + lookAtY + " pitch=" + pitchDeg);
    }

    /**
     * Safe fallback when bounding box never becomes available.
     * Works for typical Mixamo exports (centimeter scale).
     */
    private void applyFallback(ModelNode nodeRef) {
        if (nodeRef == null) return;

        nodeRef.setWorldScale(
                new Float3(0.01f, 0.01f, 0.01f)
        );

        nodeRef.setWorldPosition(
                new Float3(
                        AVATAR_X,
                        AVATAR_Y,
                        AVATAR_Z
                )
        );

        cachedScale = 0.01f;
        cachedYPos = AVATAR_Y;

        positionCamera();

        Log.d(TAG, "Fallback applied with fixed position");
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
        if (chatTabs.getSelectedTabPosition() == 1) {
            subtitleView.setText("Listening...");
        }
        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        if (speechRecognizer != null && state == AvatarState.LISTENING) {
            speechRecognizer.stopListening();
        }
    }

    // ─── Send message ─────────────────────────────────────────────────────────

    private void sendMessage(String text) {
        String trimmed = (text == null) ? "" : text.trim();
        if (trimmed.isEmpty() || waitingForOllama) return;

        messageInput.setText("");
        starterChipsScroll.setVisibility(View.GONE);

        chatMessages.add(new ChatMessage(trimmed, true));
        adapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);

        conversationHistory.add(new OllamaMessage("user", trimmed));
        waitingForOllama = true;
        setAvatarState(AvatarState.IDLE);

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
        setAvatarState(AvatarState.IDLE);
    }

    private void removeTypingIndicator() {
        int lastIndex = chatMessages.size() - 1;
        if (lastIndex >= 0 && chatMessages.get(lastIndex).isTyping()) {
            chatMessages.remove(lastIndex);
            adapter.notifyItemRemoved(lastIndex);
        }
    }

    // ─── TTS ──────────────────────────────────────────────────────────────────

    private void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (textToSpeech == null) return;
        if (!ttsReady) {
            pendingSpeechText = text;
            return;
        }
        runOnUiThread(() -> setAvatarState(AvatarState.TALKING));
        handler.postDelayed(() ->
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID), 300);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            textToSpeech.setLanguage(Locale.US);
            selectMaleVoice();
            // Male voice tuning: Lower pitch and slightly slower rate
            textToSpeech.setSpeechRate(0.88f);
            textToSpeech.setPitch(0.65f);
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    runOnUiThread(() -> {
                        if (state != AvatarState.TALKING) setAvatarState(AvatarState.TALKING);
                    });
                }
                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() ->
                            handler.postDelayed(() -> setAvatarState(AvatarState.IDLE), 400));
                }
                @Override
                public void onError(String utteranceId) {
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

    private void selectMaleVoice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || textToSpeech == null) return;

        Set<Voice> voices = textToSpeech.getVoices();
        if (voices == null || voices.isEmpty()) return;

        Voice bestMale = null;

        for (Voice voice : voices) {
            Locale locale = voice.getLocale();
            if (locale == null || !locale.getLanguage().startsWith("en")) continue;

            String name = (voice.getName() == null) ? "" : voice.getName().toLowerCase(Locale.US);

            // Comprehensive male voice detection keywords
            // Google: iom, iol, iog, sfg
            // Samsung/Generic: male, guy, man, masculine, #male
            boolean isMale = (name.contains("male") && !name.contains("female"))
                    || name.contains("guy")
                    || name.contains("man")
                    || name.contains("masculine")
                    || name.contains("#male")
                    || name.contains("iom")
                    || name.contains("iol")
                    || name.contains("iog")
                    || name.contains("sfg");

            if (isMale) {
                // Priority logic:
                // 1. Prefer offline voices (isNetworkConnectionRequired == false)
                // 2. Prefer US English (locale country == "US")
                boolean isUS = locale.getCountry().equalsIgnoreCase("US");
                boolean isOffline = !voice.isNetworkConnectionRequired();

                if (bestMale == null) {
                    bestMale = voice;
                } else {
                    boolean currentIsOffline = !bestMale.isNetworkConnectionRequired();
                    boolean currentIsUS = bestMale.getLocale().getCountry().equalsIgnoreCase("US");

                    if (isOffline && !currentIsOffline) {
                        bestMale = voice;
                    } else if (isOffline == currentIsOffline && isUS && !currentIsUS) {
                        bestMale = voice;
                    }
                }
            }
        }

        if (bestMale != null) {
            textToSpeech.setVoice(bestMale);
        }
    }

    // ─── Listening pulse ──────────────────────────────────────────────────────

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
}
package com.saad.moviessaad.ui;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.filament.Box;
import com.google.android.filament.gltfio.FilamentInstance;
import com.saad.moviessaad.R;
import dev.romainguy.kotlin.math.Float3;
import io.github.sceneview.SceneView;
import io.github.sceneview.node.ModelNode;

/**
 * Full-screen 3D avatar display.
 *
 * Camera is locked to a fixed front view at eye level — all touch events are consumed
 * so SceneView's built-in gesture detector can never rotate or pan the camera.
 *
 * Public entry points:
 *   onAiSpeechStart()  — switch avatar to talking animation (call from any thread)
 *   onAiSpeechEnd()    — return avatar to waving animation  (call from any thread)
 */
public class AvatarDisplayActivity extends AppCompatActivity {

    private static final String TAG = "AvatarDisplay";

    // ── Camera tuning — matched to AvatarChatActivity for consistent framing ──
    private static final float DESIRED_HEIGHT         = 1.8f;
    private static final float CAMERA_DISTANCE_FACTOR = 0.85f;
    private static final float CAMERA_HEIGHT_FACTOR = 1.55f;
    private static final float CAMERA_LOOKAT_FACTOR = 1.65f;
    private static final int   BOUNDS_READ_DELAY_MS   = 400;

    // How long each animation GLB plays before we reload it to create a continuous loop.
    // Tune these to match the actual animation durations in waving.glb / talking.glb.
    private static final long WAVE_LOOP_MS = 3500;
    private static final long TALK_LOOP_MS = 2000;

    private enum AvatarState { WAVING, TALKING }

    private SceneView   sceneView;
    private FrameLayout sceneContainer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ModelNode   currentModelNode;
    private AvatarState state = AvatarState.WAVING;

    // Cached framing from avatar.glb — reused when switching to animation-only GLBs.
    // waving.glb & talking.glb carry no mesh so their bounding box reads as zero.
    private float cachedScale = -1f;
    private float cachedYPos  = -999f;

    // Periodically reloads waving.glb so the wave plays continuously rather than once.
    private final Runnable waveLoopRunnable = new Runnable() {
        @Override public void run() {
            if (state == AvatarState.WAVING) {
                loadAvatarModel("waving.glb");
                handler.postDelayed(this, WAVE_LOOP_MS);
            }
        }
    };

    // Periodically reloads talking.glb so the mouth animation plays continuously.
    private final Runnable talkLoopRunnable = new Runnable() {
        @Override public void run() {
            if (state == AvatarState.TALKING) {
                loadAvatarModel("talking.glb");
                handler.postDelayed(this, TALK_LOOP_MS);
            }
        }
    };

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide action bar and status bar for a fully immersive avatar display
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_avatar_display);

        sceneContainer = findViewById(R.id.avatar_display_scene_container);
        setupSceneView();
        startAvatarSequence();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Re-apply immersive mode when the window regains focus (e.g. after dialogs)
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Call when the AI starts speaking — switches the avatar to the talking animation. */
    public void onAiSpeechStart() {
        runOnUiThread(() -> setAvatarState(AvatarState.TALKING));
    }

    /** Call when the AI finishes speaking — returns the avatar to the waving animation. */
    public void onAiSpeechEnd() {
        runOnUiThread(() -> setAvatarState(AvatarState.WAVING));
    }

    // ─── SceneView setup ──────────────────────────────────────────────────────

    private void setupSceneView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return; // Filament requires API 28+

        try {
            sceneView = new SceneView(this);

            // Consume all touch events so SceneView's gesture detector cannot rotate,
            // pan, or orbit the camera — the front view must stay completely static.
            sceneView.setOnTouchListener((v, event) -> true);

            sceneContainer.addView(sceneView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            Log.e(TAG, "SceneView init failed: " + t.getMessage());
            sceneView = null;
        }
    }

    // ─── Opening sequence ─────────────────────────────────────────────────────

    private void startAvatarSequence() {
        // Load the base mesh first so the bounding box can be read and the camera framed
        loadAvatarModel("avatar.glb");
        // Begin waving once the mesh is loaded and framed (same delay as AvatarChatActivity)
        handler.postDelayed(() -> setAvatarState(AvatarState.WAVING), 1000);
    }

    // ─── Avatar state machine ─────────────────────────────────────────────────

    private void setAvatarState(AvatarState newState) {
        state = newState;
        // Cancel any running loop from the previous state before switching
        handler.removeCallbacks(waveLoopRunnable);
        handler.removeCallbacks(talkLoopRunnable);

        switch (newState) {
            case WAVING:
                loadAvatarModel("waving.glb");
                // Schedule periodic reloads so the wave animation loops seamlessly
                handler.postDelayed(waveLoopRunnable, WAVE_LOOP_MS);
                break;
            case TALKING:
                loadAvatarModel("talking.glb");
                // Schedule periodic reloads so the talking animation loops seamlessly
                handler.postDelayed(talkLoopRunnable, TALK_LOOP_MS);
                break;
        }
    }

    // ─── GLB loading + auto-frame ─────────────────────────────────────────────
    //
    // Mirrors AvatarChatActivity.loadAvatarModel exactly so transitions are seamless:
    // the new node is pre-positioned with the cached scale/yPos before the old node
    // is removed, so there is never a frame where the avatar disappears or jumps.

    private void loadAvatarModel(String assetName) {
        if (sceneView == null) return;

        try {
            FilamentInstance modelInstance = sceneView
                    .getModelLoader()
                    .createModelInstance(assetName, resourceFileName -> null);

            if (modelInstance == null) {
                Log.e(TAG, "ModelInstance null: " + assetName);
                return;
            }

            ModelNode newNode = new ModelNode(modelInstance, true, 1.0f, new Float3(0f, 0f, 0f));
            final ModelNode oldNode = currentModelNode;

            if (!assetName.equals("avatar.glb") && cachedScale > 0f) {

                // Use exact same transform as idle avatar
                newNode.setWorldScale(
                        currentModelNode.getWorldScale()
                );

                newNode.setWorldPosition(
                        currentModelNode.getWorldPosition()
                );

                sceneView.addChildNode(newNode);

                if (oldNode != null) {
                    sceneView.removeChildNode(oldNode);
                }

                currentModelNode = newNode;

                // DO NOT reposition camera during animation swap
                return;
            }

            // avatar.glb: pre-position with cached transform so the new node enters the
            // scene already at the right size before the old node is removed.
            if (cachedScale > 0f) {
                newNode.setWorldScale(new Float3(cachedScale, cachedScale, cachedScale));
                newNode.setWorldPosition(new Float3(0f, cachedYPos, 0f));
            }
            sceneView.addChildNode(newNode);
            if (oldNode != null) sceneView.removeChildNode(oldNode);
            currentModelNode = newNode;
            if (cachedScale > 0f) positionCamera();

            final ModelNode nodeRef = newNode;
            handler.postDelayed(
                    () -> frameAvatar(nodeRef, null, assetName, 1),
                    BOUNDS_READ_DELAY_MS);

        } catch (Throwable t) {
            Log.e(TAG, "loadAvatarModel error: " + t.getMessage());
        }
    }

    private void frameAvatar(ModelNode nodeRef, ModelNode oldNode, String assetName, int attempt) {
        if (nodeRef != currentModelNode || sceneView == null) return;

        if (!assetName.equals("avatar.glb") && cachedScale > 0f) {
            if (oldNode != null) sceneView.removeChildNode(oldNode);
            return;
        }

        try {
            Box boundingBox = nodeRef.getBoundingBox();
            if (boundingBox == null) throw new IllegalStateException("null bounding box");

            float[] half   = boundingBox.getHalfExtent();
            float[] center = boundingBox.getCenter();
            float modelHeight  = half[1] * 2f;
            float modelCenterY = center[1];

            if (modelHeight < 0.001f)
                throw new IllegalStateException("height too small: " + modelHeight);

            float scale = DESIRED_HEIGHT / modelHeight;
            nodeRef.setWorldScale(new Float3(scale, scale, scale));

            // Shift vertically so the avatar's feet sit at y=0 and head at y=DESIRED_HEIGHT
            // Move avatar lower so feet stay visible and body stays centered
            float feetY = (modelCenterY - half[1]) * scale;

// small offset so feet are slightly below screen center
            float yOffset = -0.15f;

            float yPos = -feetY + yOffset;
            nodeRef.setWorldPosition(new Float3(0f, yPos, 0f));

            cachedScale = scale;
            cachedYPos  = yPos;

            if (oldNode != null) sceneView.removeChildNode(oldNode);
            positionCamera();

        } catch (Throwable t) {
            Log.w(TAG, "frameAvatar attempt " + attempt + ": " + t.getMessage());
            if (attempt < 4) {
                handler.postDelayed(
                        () -> frameAvatar(nodeRef, oldNode, assetName, attempt + 1),
                        250L * attempt);
            } else {
                if (oldNode != null) sceneView.removeChildNode(oldNode);
                applyFallback(nodeRef);
            }
        }
    }

    private void applyFallback(ModelNode nodeRef) {
        if (nodeRef == null || nodeRef != currentModelNode) return;
        // Safe fallback for Mixamo exports where bounding box is unavailable (1 unit = 1 cm)
        nodeRef.setWorldScale(new Float3(0.01f, 0.01f, 0.01f));
        nodeRef.setWorldPosition(new Float3(0f, -0.9f, 0f));
        positionCamera();
    }

    // ─── Camera lock — fixed front-facing, eye-level view ────────────────────
    //
    // The camera position is recalculated after every model load but always produces
    // the same result (it depends only on DESIRED_HEIGHT constants, never on model state).
    // Touch events are fully consumed above so this position can never be overridden.

    private void positionCamera() {
        if (sceneView == null || sceneView.getCameraNode() == null) return;

        float camY    = DESIRED_HEIGHT * CAMERA_HEIGHT_FACTOR;   // camera height in world units
        float camZ    = DESIRED_HEIGHT * CAMERA_DISTANCE_FACTOR; // distance in front of avatar
        float lookAtY = DESIRED_HEIGHT * CAMERA_LOOKAT_FACTOR;   // target height (face/chest)

        // Place camera directly in front of the avatar at a fixed distance
        sceneView.getCameraNode().setWorldPosition(new Float3(0f, camY, camZ));

        // Tilt camera upward so it looks at the avatar's face rather than the floor
        // (positive X rotation = pitch up in right-handed OpenGL coordinates)
        float deltaY   = lookAtY - camY;
        float pitchDeg = (float) Math.toDegrees(Math.atan2(deltaY, camZ));
        sceneView.getCameraNode().setRotation(new Float3(pitchDeg, 0f, 0f));
    }
}

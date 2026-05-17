package com.saad.moviessaad.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.saad.moviessaad.R;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgeScanActivity extends AppCompatActivity {

    private static final String TAG = "AgeScanActivity";
    private static final int CAMERA_PERMISSION_CODE = 1001;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_SCAN_DONE = "scan_done";
    private static final String KEY_USER_TYPE = "user_type";

    private PreviewView previewView;
    private TextView tvResult;
    private TextView tvConfidence;
    private Button btnConfirm;
    
    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;
    private String detectedType = "";
    private boolean isThresholdMet = false;
    private boolean resultLocked = false;
    
    private int consecutiveCount = 0;
    private int noFaceCount = 0;
    private String lastType = "";
    private static final int REQUIRED_CONSECUTIVE = 5;
    private static final int MAX_NO_FACE_FRAMES = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if already scanned
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SCAN_DONE, false)) {
            navigateToNext();
            return;
        }

        setContentView(R.layout.activity_age_scan);
        SystemBarInsets.applyToRoot(findViewById(android.R.id.content));

        previewView = findViewById(R.id.preview_view);
        tvResult = findViewById(R.id.tv_result);
        tvConfidence = findViewById(R.id.tv_confidence);
        btnConfirm = findViewById(R.id.btn_confirm);
        
        btnConfirm.setVisibility(View.GONE);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize ML Kit Face Detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        btnConfirm.setOnClickListener(v -> {
            if (!detectedType.isEmpty()) {
                prefs.edit()
                        .putBoolean(KEY_SCAN_DONE, true)
                        .putString(KEY_USER_TYPE, detectedType)
                        .apply();
                navigateToNext();
            }
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (faceDetector == null || resultLocked) {
            imageProxy.close();
            return;
        }

        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        noFaceCount++;
                        if (noFaceCount >= MAX_NO_FACE_FRAMES) {
                            runOnUiThread(() -> {
                                tvResult.setText("No face detected, please look at the camera...");
                                tvConfidence.setVisibility(View.GONE);
                                consecutiveCount = 0;
                            });
                        }
                    } else {
                        noFaceCount = 0;
                        estimateAge(faces.get(0));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Face detection failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void estimateAge(Face face) {
        // ML Kit doesn't provide age directly. We use multi-point facial biometrics.
        // 1. Children have a relatively shorter mid-face (eye-to-mouth).
        // 2. Children's eyes appear wider apart relative to their face width.
        // 3. Children typically have rounder faces (width/height ratio closer to 1.0).
        
        Rect bounds = face.getBoundingBox();
        float faceHeight = (float) bounds.height();
        float faceWidth  = (float) bounds.width();
        
        FaceLandmark leftEye  = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark mouth    = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);
        
        if (leftEye == null || rightEye == null || mouth == null) return;
        
        float eyeY = (leftEye.getPosition().y + rightEye.getPosition().y) / 2.0f;
        float eyeToMouthDist = mouth.getPosition().y - eyeY;
        float eyeDist = Math.abs(rightEye.getPosition().x - leftEye.getPosition().x);
        
        // Ratio A: Mid-face length relative to total face height
        // Adult average: ~0.45-0.50 | Kid average: ~0.35-0.42
        float midFaceRatio = eyeToMouthDist / faceHeight;
        
        // Ratio B: Eye spacing relative to face width
        // Kids often have wider set eyes relative to face width: ~0.45+
        float eyeSpacingRatio = eyeDist / faceWidth;
        
        // Ratio C: Face Aspect (Roundness)
        // Kids: > 0.85 (rounder) | Adults: < 0.80 (longer)
        float roundness = faceWidth / faceHeight;

        Log.d(TAG, String.format("Biometrics - Midface: %.3f, Spacing: %.3f, Roundness: %.3f", 
                midFaceRatio, eyeSpacingRatio, roundness));

        // Scoring System (higher = more likely a kid)
        float kidScore = 0;
        
        // Midface check (Strongest signal)
        if (midFaceRatio < 0.44f) kidScore += 2.0f;
        if (midFaceRatio < 0.38f) kidScore += 1.0f; // extra bonus for very short faces
        
        // Eye spacing check
        if (eyeSpacingRatio > 0.44f) kidScore += 1.0f;
        
        // Roundness check
        if (roundness > 0.88f) kidScore += 1.0f;

        String type;
        float confidence;
        
        // Threshold: 2.5 out of 5.0 points to be a Kid
        if (kidScore >= 2.5f) {
            type = "Kid";
            // Map score to 70% - 95% range
            confidence = Math.min(0.95f, 0.70f + (kidScore - 2.5f) * 0.1f);
        } else {
            type = "Adult";
            // Map score to 70% - 95% range
            confidence = Math.min(0.95f, 0.70f + (2.5f - kidScore) * 0.1f);
        }
        
        runOnUiThread(() -> updateUI(type, confidence));
    }

    private void updateUI(String type, float confidence) {
        if (isThresholdMet) return;

        if (type.equals(lastType)) {
            consecutiveCount++;
        } else {
            consecutiveCount = 0;
            lastType = type;
        }

        if (consecutiveCount > 1) {
            tvResult.setText(type + " detected...");
            tvConfidence.setVisibility(View.VISIBLE);
            tvConfidence.setText(String.format("%.1f%% confidence", confidence * 100));
        } else {
            tvResult.setText("Scanning...");
            tvConfidence.setVisibility(View.GONE);
        }

        if (confidence >= 0.70f && consecutiveCount >= REQUIRED_CONSECUTIVE) {
            isThresholdMet = true;
            resultLocked = true;
            detectedType = type.toLowerCase();
            
            tvResult.setText(type + " Confirmed");
            tvConfidence.setText("Confidence: " + String.format("%.1f%%", confidence * 100));
            
            btnConfirm.setVisibility(View.VISIBLE);
            btnConfirm.setText("Continue as " + type);
        }
    }

    private void navigateToNext() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}

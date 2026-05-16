package com.saad.moviessaad.ui;

import static androidx.core.content.ContextCompat.startActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.saad.moviessaad.R;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
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
    
    private Interpreter tflite;
    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;
    private String detectedType = "";
    private boolean isThresholdMet = false;
    private boolean resultLocked = false;
    private long cameraStartTime = 0;
    private static final long WARMUP_MS = 2000;

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

        // Reset state for a fresh start
        detectedType = "";
        isThresholdMet = false;
        resultLocked = false;
        cameraStartTime = 0;
        consecutiveCount = 0;
        lastType = "";

        previewView = findViewById(R.id.preview_view);
        tvResult = findViewById(R.id.tv_result);
        tvConfidence = findViewById(R.id.tv_confidence);
        btnConfirm = findViewById(R.id.btn_confirm);
        
        // Ensure confirm button is hidden on start
        btnConfirm.setVisibility(View.GONE);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize Face Detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = FaceDetection.getClient(options);

        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this, "model.tflite");
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            tfliteOptions.setNumThreads(4); 
            tflite = new Interpreter(tfliteModel, tfliteOptions);
        } catch (Exception e) {
            Log.e(TAG, "Error loading TFLite model", e);
            Toast.makeText(this, "Model load failed", Toast.LENGTH_SHORT).show();
        }

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
                        .setTargetResolution(new Size(224, 224))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                cameraStartTime = System.currentTimeMillis();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private int consecutiveCount = 0;
    private int noFaceCount = 0;
    private String lastType = "";
    private static final int REQUIRED_CONSECUTIVE = 5;
    private static final int MAX_NO_FACE_FRAMES = 5;

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (tflite == null || faceDetector == null || resultLocked) {
            imageProxy.close();
            return;
        }

        if (System.currentTimeMillis() - cameraStartTime < WARMUP_MS) {
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
                    Log.d(TAG, "Faces detected: " + faces.size());
                    if (faces.isEmpty()) {
                        noFaceCount++;
                        if (noFaceCount >= MAX_NO_FACE_FRAMES) {
                            runOnUiThread(() -> {
                                tvResult.setText("No face detected, please look at the camera...");
                                tvConfidence.setVisibility(View.GONE);
                                consecutiveCount = 0;
                            });
                        }
                        imageProxy.close();
                    } else {
                        noFaceCount = 0;
                        // ✅ get bitmap on MAIN thread first, then inference on background
                        runOnUiThread(() -> {
                            Bitmap bitmap = previewView.getBitmap();
                            if (bitmap == null) {
                                imageProxy.close();
                                return;
                            }
                            // ✅ copy bitmap before passing to background thread
                            Bitmap bitmapCopy = bitmap.copy(bitmap.getConfig(), false);
                            cameraExecutor.execute(() -> runTfliteInference(imageProxy, bitmapCopy));
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed", e);
                    imageProxy.close();
                });
    }

    private void runTfliteInference(@NonNull ImageProxy imageProxy, Bitmap bitmap) {
        try {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4);
            inputBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[224 * 224];
            resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0,
                    resizedBitmap.getWidth(), resizedBitmap.getHeight());

            for (int pixelValue : intValues) {
                inputBuffer.putFloat(((pixelValue >> 16) & 0xFF) / 255.0f);
                inputBuffer.putFloat(((pixelValue >> 8)  & 0xFF) / 255.0f);
                inputBuffer.putFloat(( pixelValue        & 0xFF) / 255.0f);
            }

            // ✅ 2 classes only
            float[][] output = new float[1][2];
            tflite.run(inputBuffer, output);

            float kidScore   = output[0][0]; // Kid   = index 0
            float adultScore = output[0][1]; // adult = index 1

            Log.d(TAG, "Kid: " + kidScore + " Adult: " + adultScore);
            Log.d(TAG, "consecutiveCount: " + consecutiveCount + " lastType: " + lastType);

            runOnUiThread(() -> {
                if (kidScore > adultScore) {
                    updateUI("Kid", kidScore);
                } else {
                    updateUI("Adult", adultScore);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Inference error", e);
        } finally {
            imageProxy.close();
        }
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
        if (tflite != null) {
            tflite.close();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}

package com.saad.moviessaad.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.saad.moviessaad.R;
import com.saad.moviessaad.data.SupabaseService;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileAvatar;
    private TextView tvDisplayName, tvDisplayEmail;
    private TextInputEditText etUsername, etEmail, etBio;
    private MaterialButton btnSave, btnSignOut;
    private FrameLayout loadingOverlay;
    private String userId;
    private String currentAvatarUrl = "";

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadAvatar(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = SupabaseService.INSTANCE.getCurrentUserId();
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        loadProfile();
    }

    private void initViews() {
        profileAvatar = findViewById(R.id.profile_avatar);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvDisplayEmail = findViewById(R.id.tv_display_email);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etBio = findViewById(R.id.et_bio);
        btnSave = findViewById(R.id.btn_save);
        btnSignOut = findViewById(R.id.btn_sign_out);
        loadingOverlay = findViewById(R.id.loading_overlay);

        profileAvatar.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());
        btnSignOut.setOnClickListener(v -> signOut());
    }

    private void loadProfile() {
        showLoading(true);
        SupabaseService.INSTANCE.loadProfile(userId, new SupabaseService.ProfileCallback() {
            @Override
            public void onSuccess(String username, String email, String bio, String avatarUrl) {
                showLoading(false);
                tvDisplayName.setText(username.isEmpty() ? "User" : username);
                tvDisplayEmail.setText(email);
                etUsername.setText(username);
                etEmail.setText(email);
                etBio.setText(bio);
                currentAvatarUrl = avatarUrl;
                loadAvatar(avatarUrl);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                showSnack("Error: " + message, R.color.colorError);
            }
        });
    }

    private void loadAvatar(String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(profileAvatar);
        }
    }

    private void uploadAvatar(Uri uri) {
        showLoading(true);
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            SupabaseService.INSTANCE.uploadAvatar(userId, data, new SupabaseService.AvatarUploadCallback() {
                @Override
                public void onSuccess(String publicUrl) {
                    showLoading(false);
                    currentAvatarUrl = publicUrl;
                    loadAvatar(publicUrl);
                    showSnack("Avatar uploaded", R.color.colorSuccess);
                }

                @Override
                public void onError(String message) {
                    showLoading(false);
                    showSnack("Upload failed: " + message, R.color.colorError);
                }
            });
        } catch (Exception e) {
            showLoading(false);
            showSnack("Error reading image", R.color.colorError);
        }
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        showLoading(true);
        SupabaseService.INSTANCE.updateProfile(userId, username, email, bio, currentAvatarUrl, new SupabaseService.ActionCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);
                tvDisplayName.setText(username);
                tvDisplayEmail.setText(email);
                showSnack("Profile updated", R.color.colorSuccess);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                showSnack("Save failed: " + message, R.color.colorError);
            }
        });
    }

    private void signOut() {
        SupabaseService.INSTANCE.signOut(new SupabaseService.ActionCallback() {
            @Override
            public void onSuccess() {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                showSnack("Logout failed", R.color.colorError);
            }
        });
    }

    private void showLoading(boolean loading) {
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showSnack(String message, int colorRes) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setActionTextColor(getColor(colorRes));
        snackbar.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}

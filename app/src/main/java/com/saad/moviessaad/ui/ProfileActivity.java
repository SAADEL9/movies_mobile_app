package com.saad.moviessaad.ui;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.saad.moviessaad.R;
import com.saad.moviessaad.data.SupabaseService;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileAvatar;
    private TextView tvDisplayName, tvDisplayEmail, tvDisplayBio, tvChangeAvatar;
    private TextInputEditText etUsername, etEmail, etBio;
    private TextInputLayout tilUsername, tilEmail, tilBio;
    private MaterialButton btnEdit, btnSave, btnCancelEdit, btnSignOut, btnWatched;
    private FrameLayout loadingOverlay;
    private NestedScrollView profileScroll;
    private String userId;
    private String currentAvatarUrl = "";
    private String loadedUsername = "";
    private String loadedEmail = "";
    private String loadedBio = "";
    private boolean isEditing = false;

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
        tvDisplayBio = findViewById(R.id.tv_display_bio);
        tvChangeAvatar = findViewById(R.id.tv_change_avatar);
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilBio = findViewById(R.id.til_bio);
        profileScroll = findViewById(R.id.profile_scroll);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etBio = findViewById(R.id.et_bio);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);
        btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        btnSignOut = findViewById(R.id.btn_sign_out);
        btnWatched = findViewById(R.id.btn_watched);
        loadingOverlay = findViewById(R.id.loading_overlay);

        profileAvatar.setOnClickListener(v -> {
            if (isEditing) {
                galleryLauncher.launch("image/*");
            }
        });
        btnEdit.setOnClickListener(v -> setEditing(true));
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancelEdit.setOnClickListener(v -> {
            bindProfile(loadedUsername, loadedEmail, loadedBio);
            setEditing(false);
        });
        btnSignOut.setOnClickListener(v -> signOut());
        btnWatched.setOnClickListener(v -> {
            startActivity(new Intent(this, WatchedActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
        applySystemBarPadding();
        setEditing(false);
    }

    private void applySystemBarPadding() {
        int baseBottomPadding = getResources().getDimensionPixelSize(R.dimen.profile_bottom_padding);
        ViewCompat.setOnApplyWindowInsetsListener(profileScroll, (view, insets) -> {
            int systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    Math.max(baseBottomPadding, systemBottom + getResources().getDimensionPixelSize(R.dimen.profile_bottom_padding_extra))
            );
            return insets;
        });
    }

    private void loadProfile() {
        showLoading(true);
        SupabaseService.INSTANCE.loadProfile(userId, new SupabaseService.ProfileCallback() {
            @Override
            public void onSuccess(String username, String email, String bio, String avatarUrl) {
                showLoading(false);
                loadedUsername = cleanProfileValue(username);
                loadedEmail = cleanProfileValue(email);
                loadedBio = cleanProfileValue(bio);
                bindProfile(loadedUsername, loadedEmail, loadedBio);
                currentAvatarUrl = avatarUrl;
                loadAvatar(avatarUrl);
                setEditing(false);
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                showSnack("Error: " + message, R.color.colorError);
            }
        });
    }

    private String cleanProfileValue(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.equalsIgnoreCase("null") ? "" : trimmed;
    }

    private void bindProfile(String username, String email, String bio) {
        String displayName = username == null || username.trim().isEmpty() ? "User" : username.trim();
        String displayEmail = email == null || email.trim().isEmpty() ? "No email saved" : email.trim();
        String displayBio = bio == null || bio.trim().isEmpty() ? "No bio yet." : bio.trim();
        tvDisplayName.setText(displayName);
        tvDisplayEmail.setText(displayEmail);
        tvDisplayBio.setText(displayBio);
        etUsername.setText(username);
        etEmail.setText(email);
        etBio.setText(bio);
    }

    private void setEditing(boolean editing) {
        isEditing = editing;
        int editVisibility = editing ? View.VISIBLE : View.GONE;
        int viewVisibility = editing ? View.GONE : View.VISIBLE;
        tvDisplayBio.setVisibility(viewVisibility);
        btnEdit.setVisibility(viewVisibility);
        btnWatched.setVisibility(viewVisibility);
        tvChangeAvatar.setVisibility(editVisibility);
        tilUsername.setVisibility(editVisibility);
        tilEmail.setVisibility(editVisibility);
        tilBio.setVisibility(editVisibility);
        btnSave.setVisibility(editVisibility);
        btnCancelEdit.setVisibility(editVisibility);
        profileAvatar.setClickable(editing);
        profileAvatar.setFocusable(editing);
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
                loadedUsername = username;
                loadedEmail = email;
                loadedBio = bio;
                bindProfile(loadedUsername, loadedEmail, loadedBio);
                setEditing(false);
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
                // Clear age scan data so they have to scan again
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit()
                        .remove("scan_done")
                        .remove("user_type")
                        .apply();

                Intent intent = new Intent(ProfileActivity.this, AgeScanActivity.class);
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

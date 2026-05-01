package com.saad.moviessaad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.saad.moviessaad.R;
import com.saad.moviessaad.data.SupabaseService;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        findViewById(android.R.id.content).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        MaterialButton btnRegister = findViewById(R.id.btn_register);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

        btnRegister.setOnClickListener(v -> register());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void register() {
        String username = textOf(etUsername);
        String email = textOf(etEmail);
        String password = textOf(etPassword);
        String confirmPassword = textOf(etConfirmPassword);
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showSnack("All fields are required", R.color.colorError);
            return;
        }
        if (!password.equals(confirmPassword)) {
            showSnack("Passwords do not match", R.color.colorError);
            return;
        }

        SupabaseService.INSTANCE.register(email, password, username, new SupabaseService.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                showSnack("Registration failed", R.color.colorError);
            }
        });
    }

    private String textOf(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    private void showSnack(String message, int colorRes) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setActionTextColor(getColor(colorRes));
        snackbar.show();
    }
}

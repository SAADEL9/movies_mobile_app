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

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findViewById(android.R.id.content).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        TextView tvRegisterLink = findViewById(R.id.tv_register_link);

        btnLogin.setOnClickListener(v -> login());
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SupabaseService.INSTANCE.isLoggedIn()) {
            navigateToMain();
        }
    }

    private void login() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showSnack("Please enter email and password", R.color.colorError);
            return;
        }

        SupabaseService.INSTANCE.login(email, password, new SupabaseService.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                navigateToMain();
            }

            @Override
            public void onError(String message) {
                showSnack("Login failed", R.color.colorError);
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showSnack(String message, int colorRes) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setActionTextColor(getColor(colorRes));
        snackbar.show();
    }
}

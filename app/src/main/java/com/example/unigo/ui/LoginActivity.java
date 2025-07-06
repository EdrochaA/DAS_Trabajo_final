package com.example.unigo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;
import com.example.unigo.network.ApiService;
import com.example.unigo.network.LoginResponse;
import com.example.unigo.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private Button btnVolverLogin, btnLogin;
    private EditText etUsername, etPassword;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnVolverLogin = findViewById(R.id.btnVolverLogin);
        etUsername     = findViewById(R.id.etUsername);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        tvRegister     = findViewById(R.id.tvRegister);

        // Botón Volver al MainActivity
        btnVolverLogin.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

        btnLogin.setOnClickListener(v -> loginUser());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("Campos vacíos", "Introduce usuario y contraseña.");
            return;
        }

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.loginUser(username, password).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    showErrorDialog("Error", "No se pudo conectar al servidor.");
                    return;
                }

                LoginResponse login = resp.body();
                if (login.isSuccess()) {
                    SharedPreferences.Editor editor =
                            getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit();

                    editor.putBoolean("isLoggedIn", true);
                    editor.putInt("userId", login.getId());
                    editor.putString("name", login.getNombre());
                    editor.putString("email", login.getEmail());
                    editor.putString("phone", login.getTelefono());
                    editor.putString("fotoUrl", login.getFoto());
                    editor.apply();

                    startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
                    finish();
                } else {
                    showErrorDialog("Error de login", login.getMessage());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (t instanceof java.net.UnknownHostException) {
                    showErrorDialog("Sin conexión", "Verifica tu conexión a Internet.");
                } else if (t instanceof java.net.SocketTimeoutException) {
                    showErrorDialog("Servidor ocupado", "El servidor tardó demasiado en responder.");
                } else {
                    showErrorDialog("Error desconocido", t.getMessage());
                }
                android.util.Log.e("LOGIN_ERROR", "Error técnico: ", t);
            }
        });
    }

    private void showErrorDialog(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("Aceptar", null)
                .show();
    }
}
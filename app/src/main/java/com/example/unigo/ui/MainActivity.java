package com.example.unigo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unigo.R;

public class MainActivity extends AppCompatActivity {

    private Button btnAcceder;
    private TextView enlaceRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAcceder = findViewById(R.id.btnAcceder);
        enlaceRegistro = findViewById(R.id.enlaceRegistro);

        // Ir a LoginActivity cuando se pulsa "Acceder"
        btnAcceder.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
            startActivity(intent);
        });

        // Ir a RegisterActivity desde el texto de registro
        enlaceRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}

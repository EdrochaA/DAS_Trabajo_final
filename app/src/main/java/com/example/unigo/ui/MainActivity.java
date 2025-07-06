package com.example.unigo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem; // Importar MenuItem
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast; // Importar Toast
import androidx.appcompat.app.AppCompatActivity;
import com.example.unigo.R;
import java.util.Locale;
import androidx.core.view.WindowCompat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS    = "SessionPrefs";
    private static final String KEY_LANG = "app_language";

    private Button   btnAcceder;
    private TextView enlaceRegistro;
    private TextView tvLanguage; // Este es tu selector de idioma

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Carga el idioma guardado antes de inflar la vista
        loadLocale();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de vistas y listeners
        btnAcceder     = findViewById(R.id.btnAcceder);
        enlaceRegistro = findViewById(R.id.enlaceRegistro);
        tvLanguage     = findViewById(R.id.tvLanguage);

        // Listener para el selector de idioma
        tvLanguage.setOnClickListener(v -> showLanguageChooser());

        // Botón Acceder al LoginActivity
        btnAcceder.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Enlace para ir a RegisterActivity
        enlaceRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Muestra un menú emergente para seleccionar el idioma.
     */
    private void showLanguageChooser() {
        PopupMenu popupMenu = new PopupMenu(this, tvLanguage);
        popupMenu.getMenu().add(0, 0, 0, getString(R.string.language_spanish)); // Español
        popupMenu.getMenu().add(0, 1, 1, getString(R.string.language_english)); // Inglés

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 0:
                        setLocale("es");
                        break;
                    case 1:
                        setLocale("en");
                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LANG, languageCode)
                .apply();

        recreate();
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String language = prefs.getString(KEY_LANG, "es");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

}
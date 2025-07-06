package com.example.unigo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class MainMenuActivity extends AppCompatActivity {
    private static final String PREFS    = "SessionPrefs";
    private static final String KEY_LANG = "app_language";
    private MaterialCardView cardUniversity;
    private MaterialCardView cardBike;
    private MaterialCardView cardTram;
    private MaterialCardView cardBus;
    private MaterialCardView cardWalk;
    private MaterialCardView cardProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        cardUniversity = findViewById(R.id.card_university);
        cardBike       = findViewById(R.id.card_bike);
        cardTram       = findViewById(R.id.card_tram);
        cardBus        = findViewById(R.id.card_bus);
        cardWalk       = findViewById(R.id.card_walk);
        cardProfile    = findViewById(R.id.card_profile);

        cardUniversity.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_university_selected), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, UniversityActivity.class));
        });

        cardBike.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_bike_selected), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, BikeActivity.class));
        });

        cardTram.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.toast_tram_wip), Toast.LENGTH_SHORT).show()
        );

        cardBus.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_bus_selected), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainMenuActivity.this, BusActivity.class));
        });

        cardWalk.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_walk_selected), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainMenuActivity.this, WalkActivity.class));
        });

        cardProfile.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.toast_profile_selected), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String language = prefs.getString(KEY_LANG, "es"); // “es” por defecto
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}

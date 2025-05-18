// MainMenuActivity.java
package com.example.unigo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;
import com.google.android.material.card.MaterialCardView;


public class MainMenuActivity extends AppCompatActivity {

    private MaterialCardView cardUniversity;
    private MaterialCardView cardBike;
    private MaterialCardView cardTram;
    private MaterialCardView cardBus;
    private MaterialCardView cardWalk;
    private MaterialCardView cardProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        cardUniversity = findViewById(R.id.card_university);
        cardBike       = findViewById(R.id.card_bike);
        cardTram       = findViewById(R.id.card_tram);
        cardBus        = findViewById(R.id.card_bus);
        cardWalk       = findViewById(R.id.card_walk);
        cardProfile    = findViewById(R.id.card_profile);

        cardUniversity.setOnClickListener(v -> {
            Toast.makeText(this, "Universidad seleccionada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, TransporteActivity.class);
            intent.putExtra("transportType", "bike");
            startActivity(intent);
        });


        cardBike.setOnClickListener(v -> {
            Toast.makeText(this, "Bicicleta seleccionada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, BikeActivity.class);
            intent.putExtra("transportType", "bike");
            startActivity(intent);
        });

        cardTram.setOnClickListener(v -> {
            Toast.makeText(this, "Tranvía seleccionado", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, TransporteActivity.class);
            intent.putExtra("transportType", "tram");
            startActivity(intent);
        });

        cardBus.setOnClickListener(v -> {
            Toast.makeText(this, "Autobús seleccionado", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, TransporteActivity.class);
            intent.putExtra("transportType", "bus");
            startActivity(intent);

        });

        cardWalk.setOnClickListener(v -> {
            Toast.makeText(this, "A pie seleccionado", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, TransporteActivity.class);
            intent.putExtra("transportType", "walk");
            startActivity(intent);
        });

        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }
}

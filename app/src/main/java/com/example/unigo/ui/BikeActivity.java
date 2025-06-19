package com.example.unigo.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.DashPathEffect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.example.unigo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Locale;

public class BikeActivity extends AppCompatActivity {
    private static final int REQUEST_PERMS = 123;
    private static final int REQUEST_LOCATION = 1;

    private static final GeoPoint CAMPUS_LOCATION = new GeoPoint(42.839448, -2.670349);
    private static final float MAX_DIST_METERS = 50_000f;

    private MapView map;
    private IMapController mapController;
    private FusedLocationProviderClient locClient;
    private ImageButton btnBack;
    private TextView tvInfo;

    private GeoPoint ubicacionAleatoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(
                this,
                PreferenceManager.getDefaultSharedPreferences(this)
        );
        setContentView(R.layout.activity_walk);

        btnBack = findViewById(R.id.btn_back);
        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setVisibility(View.GONE);

        if (btnBack != null) {
            btnBack.bringToFront();
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(14.5);

        ubicacionAleatoria = generarUbicacionDentroVitoria();
        mapController.setCenter(ubicacionAleatoria);

        locClient = LocationServices.getFusedLocationProviderClient(this);

        if (!hasLocationPermissions()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_PERMS
            );
        } else {
            setupMap();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setupMap() {
        // Usamos directamente la ubicación aleatoria simulada
        GeoPoint start = ubicacionAleatoria;
        addUserMarker(start);
        addDestMarker(CAMPUS_LOCATION, "Campus Álava");
        drawRoute(start, CAMPUS_LOCATION);
    }

    private void addUserMarker(GeoPoint p) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle("Ubicación simulada");
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
    }

    private void addDestMarker(GeoPoint p, String title) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(title);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        m.setIcon(getResources().getDrawable(R.drawable.ic_campus_red, getTheme()));
        map.getOverlays().add(m);
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        OSRMRoadManager roadManager = new OSRMRoadManager(this, "UNIGO");
        roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(start);
        waypoints.add(end);

        new Thread(() -> {
            Road road = roadManager.getRoad(waypoints);

            runOnUiThread(() -> {
                if (road.mStatus != Road.STATUS_OK) {
                    Log.e("WalkActivity", "Error al calcular ruta, status=" + road.mStatus);
                    return;
                }

                Polyline overlay = RoadManager.buildRoadOverlay(road);
                Paint paint = overlay.getPaint();
                paint.setColor(Color.RED);
                paint.setStrokeWidth(8f);
                paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

                map.getOverlays().add(overlay);
                map.invalidate();

                double distanciaKm = road.mLength;
                int minutos = (int) Math.round((distanciaKm / 5.0) * 60);
                String info = String.format(
                        Locale.getDefault(),
                        "Dist: %.2f km\nTiempo: %d min",
                        distanciaKm, minutos
                );
                tvInfo.setText(info);
                tvInfo.setVisibility(View.VISIBLE);
                tvInfo.bringToFront();
                tvInfo.invalidate();
                tvInfo.requestLayout();
            });
        }).start();
    }

    private GeoPoint generarUbicacionDentroVitoria() {
        double minLat = 42.82, maxLat = 42.87;
        double minLon = -2.71, maxLon = -2.64;
        double lat = minLat + Math.random() * (maxLat - minLat);
        double lon = minLon + Math.random() * (maxLon - minLon);
        return new GeoPoint(lat, lon);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == REQUEST_PERMS || requestCode == REQUEST_LOCATION)
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap();
        } else {
            Toast.makeText(
                    this,
                    "Permiso de ubicación denegado",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
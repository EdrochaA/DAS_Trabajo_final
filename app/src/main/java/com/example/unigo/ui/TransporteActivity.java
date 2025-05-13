package com.example.unigo.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.unigo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class TransporteActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 1;

    private MapView map;
    private List<GeoPoint> puntos = new ArrayList<>();
    private double distanciaTotal = 0.0;
    private String transportType;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView distanciaTotalTextView;
    private Polyline currentRouteLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configuración inicial de OSM
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_transporte);

        // Inicializar componentes
        distanciaTotalTextView = findViewById(R.id.distancia_total);
        map = findViewById(R.id.map);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurar mapa
        configurarMapa();
        inicializarPolyline();
        checkGPSYPermisos();

        // Obtener tipo de transporte
        transportType = getIntent().getStringExtra("transportType");
        añadirListeners();
    }

    private void checkGPSYPermisos() {
        if (!isGPSEnabled()) {
            Toast.makeText(this, "Activa el GPS para continuar", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
        requestPermissionsIfNecessary();
    }

    private void configurarMapa() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);
    }
    

    private void inicializarPolyline() {
        currentRouteLine = new Polyline();
        currentRouteLine.setColor(getColorForTransport(transportType));
        map.getOverlays().add(currentRouteLine);
    }

    private int getColorForTransport(String transportType) {
        switch (transportType) {
            case "bike": return Color.parseColor("#FF00FF00"); // Verde
            case "bus": return Color.parseColor("#FFFF0000");    // Rojo
            case "tram": return Color.parseColor("#FF0000FF");   // Azul
            default: return Color.parseColor("#FF9C27B0");       // Morado para caminar
        }
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        Location location = locationResult.getLastLocation();
                        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        añadirPuntoARuta(currentPoint);
                    }
                }
            };
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void añadirPuntoARuta(GeoPoint punto) {
        puntos.add(punto);

        // Añadir marcador
        Marker marker = new Marker(map);
        marker.setPosition(punto);
        marker.setTitle("Punto " + puntos.size());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);

        // Actualizar linea de ruta
        if (puntos.size() > 1) {
            currentRouteLine.setPoints(puntos);

            GeoPoint anterior = puntos.get(puntos.size() - 2);
            double distanciaSegmento = anterior.distanceToAsDouble(punto);
            distanciaTotal += distanciaSegmento;

            // Actualizar UI
            distanciaTotalTextView.setText(String.format("Distancia total: %.0f m", distanciaTotal));
            Toast.makeText(this, "Nuevo punto añadido: +" + (int) distanciaSegmento + " m", Toast.LENGTH_SHORT).show();
        }
        map.invalidate();
    }

    private void añadirListeners() {
        MapEventsOverlay overlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                añadirPuntoARuta(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        map.getOverlays().add(0, overlay);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permisos necesarios para funcionar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestPermissionsIfNecessary() {
        List<String> permissionsToRequest = new ArrayList<>();
        String[] requiredPermissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_NETWORK_STATE
        };

        for (String perm : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
        } else {
            startLocationUpdates();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


}

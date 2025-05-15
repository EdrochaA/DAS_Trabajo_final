package com.example.unigo.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.unigo.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import java.util.ArrayList;
import java.util.List;

public class TransporteActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;

    private MapView map;
    private TextView distanciaTotalTextView;
    private String transportType;

    private List<GeoPoint> puntos = new ArrayList<>();
    private double distanciaTotal = 0.0;
    private Polyline currentRouteLine;
    private MyLocationNewOverlay locationOverlay;
    private ItemizedIconOverlay mMyLocationsOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_transporte);

        transportType = getIntent().getStringExtra("transportType");

        map = findViewById(R.id.map);
        distanciaTotalTextView = findViewById(R.id.distancia_total);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(16.0);
        map.getController().setCenter(new GeoPoint(42.8467, -2.6731));
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);

        añadirMarcadoresUniversidad();
        inicializarPolyline();
        checkGPSYPermisos();
        añadirListeners();
        añadirTeselas();
        calcularRutaOptima();

    }

    private void añadirTeselas() {
        switch (transportType) {
            case "bike":
                return ;
            case "bus":
                return ;
            case "tram":
                return ;
            case "walk":
                return ;
        }
    }

    private void calcularRutaOptima() {
        switch (transportType) {
            case "bike":
                return;
            case "bus":
                return;
            case "tram":
                return;
            case "walk":
                return;
        }
    }

    private void checkGPSYPermisos() {
        if (!isGPSEnabled()) {
            Toast.makeText(this, "GPS desactivado. Actívalo y vuelve a intentarlo.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            finish();
            return;
        }

        requestPermissionsIfNecessary();
    }

    private boolean isGPSEnabled() {
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    private void requestPermissionsIfNecessary() {
        List<String> permissionsToRequest = new ArrayList<>();
        String[] requiredPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
            iniciarOverlayUbicacion();
        }
    }

    private void iniciarOverlayUbicacion() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        map.getOverlays().add(locationOverlay);
    }

    private void inicializarPolyline() {
        currentRouteLine = new Polyline();
        currentRouteLine.setColor(getColorForTransport(transportType));
        map.getOverlays().add(currentRouteLine);
    }

    private int getColorForTransport(String transportType) {
        switch (transportType) {
            case "bike":
                return 0xFF4CAF50; // Verde
            case "bus":
                return 0xFFF44336; // Rojo
            case "tram":
                return 0xFF2196F3; // Azul
            case "walk":
                return 0xFF9C27B0; // Morado (a pie)
        }
        return 0x000000; // Negro
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
                puntos.clear();
                distanciaTotal = 0.0;
                distanciaTotalTextView.setText("Distancia total: 0 m");

                // Limpiar marcadores y líneas, pero mantener overlays como el de ubicación
                map.getOverlays().removeIf(overlay -> overlay instanceof Marker || overlay instanceof Polyline);

                // Crear nueva línea vacía
                currentRouteLine = new Polyline();
                currentRouteLine.setColor(getColorForTransport(transportType));
                map.getOverlays().add(currentRouteLine);

                Toast.makeText(getApplicationContext(), "Ruta reiniciada", Toast.LENGTH_SHORT).show();
                map.invalidate();
                return true;
            }
        });

        map.getOverlays().add(overlay);
    }

    private void añadirPuntoARuta(GeoPoint punto) {
        puntos.add(punto);

        // Añadir marcador
        añadirMarcador(punto);

        // Actualizar línea de ruta
        if (puntos.size() > 1) {
            GeoPoint anterior = puntos.get(puntos.size() - 2);
            double distancia = anterior.distanceToAsDouble(punto);
            distanciaTotal += distancia;
            distanciaTotalTextView.setText(String.format("Distancia total: %.0f m", distanciaTotal));
        }

        currentRouteLine.setPoints(new ArrayList<>(puntos));
        map.invalidate();
    }

    private void añadirMarcador(GeoPoint punto) {
        Marker marker = new Marker(map);
        marker.setPosition(punto);
        int nPuntos = puntos.size()-1;
        marker.setTitle("Punto " + nPuntos);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
    }

    private void añadirMarcadoresUniversidad() {
//        GeoPoint universidad = new GeoPoint(42.83961571186042, -2.670334245035063);
//        Marker marker = new Marker(map);
//        marker.setPosition(universidad);
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        marker.setTitle("Campus UPV/EHU");
//        marker.setSnippet("Destino: Universidad del País Vasco");
//        marker.setIcon(getResources().getDrawable(R.drawable.ic_school, getTheme()));
//        map.getOverlays().add(marker);
        final ArrayList<OverlayItem> items = new ArrayList<>();
        items.add(new OverlayItem("Universidad UPV/EHU", "Edificio principal",
                new GeoPoint(42.83961571186042, -2.670334245035063)));
        items.add(new OverlayItem("Biblioteca", "Biblioteca de la universidad",
                new GeoPoint(42.838115043555135, -2.67372868068938)));
        items.add(new OverlayItem("Letras", "Facultad de Letras",
                new GeoPoint(42.84027241106035, -2.671013465912215)));

        this.mMyLocationsOverlay = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        return false;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, getApplicationContext());
        this.map.getOverlays().add(this.mMyLocationsOverlay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarOverlayUbicacion();
            } else {
                Toast.makeText(this, "Permisos necesarios no concedidos", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}

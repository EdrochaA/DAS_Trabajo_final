package com.example.unigo.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.unigo.R;

import org.mapsforge.core.graphics.Paint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.wms.WMSEndpoint;
import org.osmdroid.wms.WMSParser;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mil.nga.sf.geojson.FeatureCollection;

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
    private IMapController mMyMapController;
    private GeoPoint upvGazteiz = new GeoPoint(42.83948958833751, -2.670196062086965);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_transporte);

        transportType = getIntent().getStringExtra("transportType");

        map = findViewById(R.id.map);
        distanciaTotalTextView = findViewById(R.id.distancia_total);

        requestPermissionsIfNecessary();

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(20.0);
        map.getController().setCenter(new GeoPoint(upvGazteiz));
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);

        mMyMapController = map.getController();

        añadirMarcadoresUniversidad();
        inicializarPolyline();
        checkGPSYPermisos();
        añadirListeners();

        ImageButton mUbiImageButton = findViewById(R.id.iconUbi);
        ImageButton mUniImageButton = findViewById(R.id.iconUni);
        mUbiImageButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               mMyMapController.animateTo(locationOverlay.getMyLocation());
               mMyMapController.setZoom(18.0);

           }
       });
        mUniImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMyMapController.animateTo(upvGazteiz);
                map.getController().setZoom(17.0);

            }
        });

        añadirCaminos();
        calcularRutaOptima();

    }

    private void añadirCaminos() {
        switch (transportType) {
            case "bike":
                String jsonString = null;
                try {
                    InputStream jsonStream = getAssets().open("bidegorris23.geojson");
                    int size = jsonStream.available();
                    byte[] buffer = new byte[size];
                    jsonStream.read(buffer);
                    jsonStream.close();
                    jsonString = new String(buffer, "UTF-8");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }

                KmlDocument kmlDocument = new KmlDocument();
                kmlDocument.parseGeoJSON(jsonString);
                FolderOverlay myOverLay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);
                map.getOverlays().add(myOverLay);
                map.invalidate();

                break;


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
                GeoPoint ubicacionActual = locationOverlay.getMyLocation();
                //GeoPoint ubicacionActual = new GeoPoint(42.86464948224437, -2.6806513653849975);

                if (ubicacionActual == null) {
                    Toast.makeText(this, "Esperando ubicación GPS...", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Llamada en segundo plano para no congelar la UI
                new Thread(() -> {
                    ArrayList<GeoPoint> waypoints = new ArrayList<>();
                    waypoints.add(ubicacionActual);
                    waypoints.add(upvGazteiz);

                    RoadManager roadManager = new OSRMRoadManager(this, "UnigoApp/1.0");
                    ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE);

                    Road road = roadManager.getRoad(waypoints);

                    // Pasos de la ruta
                    Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
                    for (int i=0; i<road.mNodes.size(); i++){
                        RoadNode node = road.mNodes.get(i);
                        Marker nodeMarker = new Marker(map);
                        nodeMarker.setPosition(node.mLocation);
                        nodeMarker.setIcon(nodeIcon);
                        nodeMarker.setTitle("Step "+i);
                        map.getOverlays().add(nodeMarker);
                    }

                    runOnUiThread(() -> {
                        if (road.mStatus != Road.STATUS_OK) {
                            Toast.makeText(this, "Error al calcular la ruta", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                        roadOverlay.setColor(Color.parseColor("#4CAF50"));
                        roadOverlay.setWidth(10f);

                        map.getOverlays().add(roadOverlay);
                        map.getController().setCenter(ubicacionActual);
                        map.getController().setZoom(16.0);
                        map.invalidate();

                        // Mostrar distancia y duración
                        double km = road.mLength;
                        distanciaTotalTextView.setText("Distancia total: \n" + String.format("%.2f", km) + " km");
                        double minutos = road.mDuration / 60.0;
                        Toast.makeText(this,
                                String.format("Distancia: %.2f km\nDuración estimada: %.1f min", km, minutos),
                                Toast.LENGTH_LONG).show();
                    });

                }).start();

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
        locationOverlay.setDrawAccuracyEnabled(true);
        locationOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (locationOverlay.getMyLocation() != null) {
                            mMyMapController.animateTo(locationOverlay.getMyLocation());
                            mMyMapController.setZoom(12.0);
                        } else {
                            Log.e("MainActivity", "La ubicación sigue siendo null");
                        }
                    }
                });
            }
        });
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
                //añadirPuntoARuta(p);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                puntos.clear();
                distanciaTotal = 0.0;
                distanciaTotalTextView.setText("Distancia total: 0 m");

                // Limpiar marcadores y líneas, pero mantener overlays
                map.getOverlays().removeIf(overlay -> overlay instanceof Marker || overlay instanceof Polyline);

                Toast.makeText(getApplicationContext(), "Ruta reiniciada", Toast.LENGTH_LONG).show();
                map.invalidate();
                calcularRutaOptima();
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
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
    }

    private void añadirMarcadoresUniversidad() {
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
                        Toast.makeText(TransporteActivity.this, item.getTitle()+", "+ item.getSnippet(), Toast.LENGTH_SHORT).show();
                        return true;
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
}

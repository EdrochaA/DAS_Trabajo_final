package com.example.unigo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.unigo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BikeActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 1;
    private MapView map;
    private IMapController mMyMapController;
    private GeoPoint ubicacionActual;
    private GeoPoint campus = new GeoPoint(42.8386, -2.6733);
    private KmlDocument kmlDocument;
    private TextView distanciaTotalTextView;
    private Polygon bidegorrisPolygone;
    private FusedLocationProviderClient fusedLocationClient;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_bike);

        distanciaTotalTextView = findViewById(R.id.dist_total);
        findViewById(R.id.btn_atras).setOnClickListener(v -> finish());
        map = findViewById(R.id.bici_map);
        progressBar = findViewById(R.id.progressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        inicializarPolygone();
        checkGPSYPermisos();
    }

    private void checkGPSYPermisos() {
        if (!isGPSEnabled()) {
            Toast.makeText(this, getString(R.string.gps_disabled), Toast.LENGTH_LONG).show();
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

    private void inicializarPolygone() {
        List<GeoPoint> geoPoints = new ArrayList<>();
        geoPoints.add(new GeoPoint(42.9246189049327, -2.7645106233091723));
        geoPoints.add(new GeoPoint(42.9246189049327, -2.5439106461168732));
        geoPoints.add(new GeoPoint(42.81934243944316, -2.5439106461168732));
        geoPoints.add(new GeoPoint(42.81934243944316, -2.7645106233091723));

        bidegorrisPolygone = new Polygon();
        bidegorrisPolygone.getFillPaint().setColor(Color.parseColor("#1EFFE70E"));
        bidegorrisPolygone.setPoints(geoPoints);
        bidegorrisPolygone.setStrokeWidth(2);
        bidegorrisPolygone.setStrokeColor(0xCCF44336);
        bidegorrisPolygone.setTitle(getString(R.string.bidegorris_araba_title));

        map.getOverlayManager().add(bidegorrisPolygone);
        map.invalidate();
    }

    private void requestPermissionsIfNecessary() {
        String[] requiredPermissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        List<String> permissionsToRequest = new ArrayList<>();
        for (String perm : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            obtenerUbicacionYCalcularRuta();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYCalcularRuta();
            } else {
                Toast.makeText(this, getString(R.string.permissions_not_granted), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionYCalcularRuta() {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, getString(R.string.getting_location), Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(this, location -> {
            if (location != null) {
                this.ubicacionActual = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.d("BikeActivity", "Ubicación real obtenida: " + ubicacionActual);
                cargarBidegorris();
                inicializarMapa();
                añadirMarkerInicial();
                marcarCampus();
                añadirParkingYReparacion();
                añadirParkingsSeguros();
                dibujarBidegorris();
                calcularRuta();
            } else {
                Toast.makeText(this, getString(R.string.location_not_found_gps), Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void calcularRuta() {
        if (ubicacionActual == null) {
            Toast.makeText(this, getString(R.string.location_not_available), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }
        new Thread(() -> {
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(ubicacionActual);
            waypoints.add(campus);

            RoadManager roadManager = new OSRMRoadManager(this, "UnigoApp/1.0");
            ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE);

            Road road = roadManager.getRoad(waypoints);

            runOnUiThread(() -> {
                if (road.mStatus != Road.STATUS_OK) {
                    Toast.makeText(this, getString(R.string.error_calculating_bike_route), Toast.LENGTH_LONG).show();
                } else {
                    Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                    roadOverlay.setColor(0x80333333);
                    roadOverlay.setWidth(10f);
                    map.getOverlays().add(roadOverlay);
                    map.invalidate();

                    double km = road.mLength;
                    distanciaTotalTextView.setText(getString(R.string.bike_total_distance_format, km));

                    double minutos = road.mDuration / 60.0;
                    String details = getString(R.string.bike_route_details_format, km, minutos);
                    Toast.makeText(this, details, Toast.LENGTH_LONG).show();
                }
                progressBar.setVisibility(View.GONE);
            });

        }).start();
    }

    private void añadirParkingsWSM() {
        try {
            InputStream is = getAssets().open("vgbiziz.geojson");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject geojson = new JSONObject(jsonBuilder.toString());
            JSONArray features = geojson.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                String type = geometry.getString("type");

                if ("Point".equals(type)) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    double lon = coordinates.getDouble(0);
                    double lat = coordinates.getDouble(1);
                    GeoPoint point = new GeoPoint(lat, lon);
                    JSONObject properties = feature.getJSONObject("properties");
                    String nombre = properties.optString("nombre", getString(R.string.default_secure_parking));
                    String calle = properties.optString("direccion", getString(R.string.address_unknown));

                    Marker marker = new Marker(map);
                    marker.setPosition(point);
                    marker.setTitle(nombre);
                    marker.setSubDescription(calle);
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_circulo_azul, getTheme()));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(marker);
                }
            }
            map.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_loading_parkings), Toast.LENGTH_LONG).show();
        }
    }

    private void marcarCampus() {
        Marker marker = new Marker(map);
        marker.setPosition(campus);
        marker.setTitle(getString(R.string.campus_upv_ehu));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_university, getTheme()));
        map.getOverlays().add(marker);
    }

    private void dibujarBidegorris() {
        if (kmlDocument == null) return;
        Drawable defaultMarker = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_node, getTheme());
        Bitmap defaultBitmap = ((BitmapDrawable) defaultMarker).getBitmap();
        Style defaultStyle = new Style(defaultBitmap, 0xCCF44336, 6.0f, 0xFFFFFFFF);
        FolderOverlay myOverLay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, null, kmlDocument);
        map.getOverlays().add(myOverLay);
        map.invalidate();
    }

    private void añadirParkingsSeguros() {
        final ArrayList<OverlayItem> items = new ArrayList<>();
        String titulo = getString(R.string.secure_parking_network_title);
        items.add(new OverlayItem(titulo, "Arriaga-Lakua", new GeoPoint(42.864756, -2.680381)));
        items.add(new OverlayItem(titulo, "Estacion de Autobuses", new GeoPoint(42.858083, -2.685410)));
        // ... (resto de paradas) ...
        items.add(new OverlayItem(titulo, "Iturritxu", new GeoPoint(42.835151, -2.667759)));

        Drawable parkingIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_circulo_azul, null);
        Bitmap parkingBitmap = ((BitmapDrawable) parkingIcon).getBitmap();
        Bitmap scaledParkingBitmap = Bitmap.createScaledBitmap(parkingBitmap, 16, 16, false);
        Drawable scaledParkingIcon = new BitmapDrawable(getResources(), scaledParkingBitmap);

        for (OverlayItem item : items) {
            item.setMarker(scaledParkingIcon);
        }
        ItemizedIconOverlay marcadoresParking = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        Toast.makeText(BikeActivity.this, item.getTitle() + "\n" + item.getSnippet(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) { return false; }
                }, getApplicationContext());
        map.getOverlays().add(marcadoresParking);
    }

    private void añadirParkingYReparacion() {
        final ArrayList<OverlayItem> items = new ArrayList<>();
        items.add(new OverlayItem(getString(R.string.bike_repair_point_title), "FERMIN LASUEN, 1", new GeoPoint(42.855289, -2.670029)));
        items.add(new OverlayItem(getString(R.string.bike_repair_point_title), "FRANCISCO JAVIER DE LANDABURU", new GeoPoint(42.862555, -2.683456)));
        items.add(new OverlayItem(getString(R.string.memorial_bike_parking_title), "OLAGUIBEL", new GeoPoint(42.846453, -2.671215)));

        Drawable repairIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bike_repar, null);
        Bitmap repairBitmap = ((BitmapDrawable) repairIcon).getBitmap();
        Bitmap scaledRepairBitmap = Bitmap.createScaledBitmap(repairBitmap, 32, 32, false);
        Drawable scaledRepairIcon = new BitmapDrawable(getResources(), scaledRepairBitmap);

        Drawable parkingIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_parking_bici, null);
        Bitmap parkingBitmap = ((BitmapDrawable) parkingIcon).getBitmap();
        Bitmap scaledParkingBitmap = Bitmap.createScaledBitmap(parkingBitmap, 32, 32, false);
        Drawable scaledParkingIcon = new BitmapDrawable(getResources(), scaledParkingBitmap);

        for (OverlayItem item : items) {
            if (item.getTitle().contains("reparación") || item.getTitle().contains("repair")) {
                item.setMarker(scaledRepairIcon);
            } else {
                item.setMarker(scaledParkingIcon);
            }
        }
        ItemizedIconOverlay marcadoresParking = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        Toast.makeText(BikeActivity.this, item.getTitle() + "\n" + item.getSnippet(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) { return false; }
                }, getApplicationContext());
        map.getOverlays().add(marcadoresParking);
    }

    private void añadirMarkerInicial() {
        if (ubicacionActual == null) return;
        Marker marker = new Marker(map);
        marker.setPosition(ubicacionActual);
        marker.setTitle(getString(R.string.your_location));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_map_marker));
        map.getOverlays().add(marker);
    }

    private void cargarBidegorris() {
        String jsonString;
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
        kmlDocument = new KmlDocument();
        kmlDocument.parseGeoJSON(jsonString);
    }

    private void inicializarMapa() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        mMyMapController = map.getController();

        if (ubicacionActual != null) {
            mMyMapController.animateTo(ubicacionActual);
            map.getController().setZoom(16.0);
            map.getController().setCenter(ubicacionActual);
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
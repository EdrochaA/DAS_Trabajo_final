package com.example.unigo.ui;

import android.Manifest;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.unigo.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BikeActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 1;
    private MapView map;
    private MyLocationNewOverlay locationOverlay;
    private IMapController mMyMapController;
    private GeoPoint ubicacionAleatoria;
    private Polygon vitoriaPolygon;
    private GeoPoint campus = new GeoPoint(42.8467, -2.6731);
    private List<GeoPoint> aparcamientosSeguros = new ArrayList<GeoPoint>();
    private List<GeoPoint> puntosDeReparacion = new ArrayList<GeoPoint>();
    private BoundingBox bicisBB;
    private KmlDocument kmlDocument;
    private TextView distanciaTotalTextView;
    private Polygon bidegorrisPolygone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializar mapa
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_bike);
        distanciaTotalTextView = findViewById(R.id.dist_total);
        findViewById(R.id.btn_atras).setOnClickListener(v -> finish());

        // Configuración del mapa
        map = findViewById(R.id.bici_map);
        inicializarPolygone();
        checkGPSYPermisos();


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

    private void inicializarPolygone() {
        List<GeoPoint> geoPoints = new ArrayList<>();
        // Coordenadas del polígono de Bidegorris
        geoPoints.add(new GeoPoint(42.9246189049327, -2.7645106233091723));
        geoPoints.add(new GeoPoint(42.9246189049327, -2.5439106461168732));
        geoPoints.add(new GeoPoint(42.81934243944316, -2.5439106461168732));
        geoPoints.add(new GeoPoint(42.81934243944316, -2.7645106233091723));

        bidegorrisPolygone = new Polygon();
        bidegorrisPolygone.getFillPaint().setColor(Color.parseColor("#1EFFE70E"));
        bidegorrisPolygone.setPoints(geoPoints);
        bidegorrisPolygone.setStrokeWidth(2);
        bidegorrisPolygone.setStrokeColor(0xCCF44336); // Color del borde (rojo)
        bidegorrisPolygone.setTitle("Bidegorris Araba-Gazteiz");

        map.getOverlayManager().add(bidegorrisPolygone);
        map.invalidate();

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
            cargarBidegorris();
            ubicacionAleatoria = generarUbicacionDentroVitoria();
            inicializarMapa();
            añadirMarkerInicial();
            marcarCampus();
            añadirParkingYReparacion();
            añadirParkingsSeguros();
            dibujarBidegorris();
            calcularRuta();
        }
    }

    private void calcularRuta() {
        if (ubicacionAleatoria == null) {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        // Llamada en segundo plano para no congelar la UI
        new Thread(() -> {
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(ubicacionAleatoria);
            waypoints.add(campus);

            RoadManager roadManager = new OSRMRoadManager(this, "UnigoApp/1.0");
            ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE);

            Road road = roadManager.getRoad(waypoints);

            // Pasos de la ruta
            Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
            Drawable firstIcon = getResources().getDrawable(R.drawable.ic_position);
            for (int i=0; i<road.mNodes.size(); i++){
                RoadNode node = road.mNodes.get(i);
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(node.mLocation);
                nodeMarker.setIcon(nodeIcon);
                if (i == 0) {nodeMarker.setIcon(firstIcon);};
                nodeMarker.setTitle("Step "+i);
                map.getOverlays().add(nodeMarker);
            }

            runOnUiThread(() -> {
                if (road.mStatus != Road.STATUS_OK) {
                    Toast.makeText(this, "Error al calcular la ruta", Toast.LENGTH_LONG).show();
                    return;
                }

                Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                roadOverlay.setColor(0x80333333);
                roadOverlay.setWidth(10f);

                map.getOverlays().add(roadOverlay);
                map.getController().setCenter(ubicacionAleatoria);
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
    }

    private void añadirParkingsWSM() {
        // Extraer la capa 21, https://www.vitoria-gasteiz.org/arcgis/services/internet/10_capas_geovitoria/MapServer/WMSServer?SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0
        String wfsUrl = "https://www.vitoria-gasteiz.org/arcgis/services/internet/10_capas_geovitoria/MapServer/WFSServer" +
                "?SERVICE=WFS" +
                "&VERSION=2.0.0" +
                "&REQUEST=GetFeature" +
                "&TYPENAME=21" +
                "&OUTPUTFORMAT=application/json";

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
                    String nombre = properties.optString("nombre", "Parking seguro");
                    String calle = properties.optString("direccion", "Dirección desconocida");

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
            Toast.makeText(this, "Error al cargar los parkings seguros", Toast.LENGTH_LONG).show();
        }
    }

    private void marcarCampus() {
        Marker marker = new Marker(map);
        marker.setPosition(campus);
        marker.setTitle("Campus UPV/EHU");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_university, getTheme()));
        map.getOverlays().add(marker);
    }

    private void dibujarBidegorris() {
        Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_node);
        Bitmap defaultBitmap = ((BitmapDrawable)defaultMarker).getBitmap();
        Style defaultStyle = new Style(defaultBitmap, 0xCCF44336, 6.0f, 0xFFFFFFFF);
        FolderOverlay myOverLay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map, defaultStyle, null, kmlDocument);
        map.getOverlays().add(myOverLay);
        map.invalidate();

    }
    private void añadirParkingsSeguros() {
        final ArrayList<OverlayItem> items = new ArrayList<>();
        String titulo = "VGbiziz: Red de aparcamientos seguros para bicicletas";
        // Datos extraídos del servidor WSM capa 21 mediante QGIS
        items.add(new OverlayItem(
                titulo,
                "Arriaga-Lakua",
                new GeoPoint(42.864756,-2.680381)
        ));

        items.add(new OverlayItem(
                titulo,
                "Estacion de Autobuses",
                new GeoPoint(42.858083,-2.685410)
        ));

        items.add(new OverlayItem(
                titulo,
                "HUA",
                new GeoPoint(42.852185,-2.691866)
        ));
        items.add(new OverlayItem(
                titulo,
                "Aldabe",
                new GeoPoint(42.852123,-2.674484)
        ));
        items.add(new OverlayItem(
                titulo,
                "Zapatería",
                new GeoPoint(42.849702,-2.673987)
        ));
        items.add(new OverlayItem(
                titulo,
                "Correría",
                new GeoPoint(42.848895,-2.673615)
        ));
        items.add(new OverlayItem(
                titulo,
                "Los Herrán",
                new GeoPoint(42.849420,-2.666201)
        ));
        items.add(new OverlayItem(
                titulo,
                "Arana",
                new GeoPoint(42.849358,-2.659160)
        ));
        items.add(new OverlayItem(
                titulo,
                "Nueva Fuera",
                new GeoPoint(42.847737,-2.669130)
        ));
        items.add(new OverlayItem(
                titulo,
                "Memorial",
                new GeoPoint(42.846117,-2.671436)
        ));
        items.add(new OverlayItem(
                titulo,
                "Hospital de Santiago",
                new GeoPoint(42.845993,-2.666015)
        ));
        items.add(new OverlayItem(
                titulo,
                "Santa Bárbara",
                new GeoPoint(42.844622,-2.667448)
        ));
        items.add(new OverlayItem(
                titulo,
                "Ariznabarra",
                new GeoPoint(42.841631,-2.691437)
        ));
        items.add(new OverlayItem(
                titulo,
                "Mendizorrotza",
                new GeoPoint(42.837643,-2.687013)
        ));
        items.add(new OverlayItem(
                titulo,
                "Campus de Álava",
                new GeoPoint(42.840447,-2.671311)
        ));
        items.add(new OverlayItem(
                titulo,
                "Adurtza",
                new GeoPoint(42.836459,-2.663958)
        ));
        items.add(new OverlayItem(
                titulo,
                "Iturritxu",
                new GeoPoint(42.835151,-2.667759)
        ));


        // Configurar iconos personalizados
        Drawable parkingIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_circulo_azul, null);

        Bitmap parkingBitmap = ((BitmapDrawable) parkingIcon).getBitmap();
        Bitmap scaledParkingBitmap = Bitmap.createScaledBitmap(parkingBitmap, 16, 16, false);
        Drawable scaledParkingIcon = new BitmapDrawable(getResources(), scaledParkingBitmap);

        // Asignar iconos según el tipo de marcador
        for (OverlayItem item : items) {
            item.setMarker(scaledParkingIcon);
        }
        // Crear el overlay con los items y el listener de clics
        ItemizedIconOverlay marcadoresParking = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        Toast.makeText(
                                BikeActivity.this,
                                item.getTitle(),
                                Toast.LENGTH_SHORT
                        ).show();
                        Toast.makeText(
                                BikeActivity.this,
                                item.getSnippet(),
                                Toast.LENGTH_SHORT
                        ).show();
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                },
                getApplicationContext()
        );

        // Añadir los marcadores al mapa
        map.getOverlays().add(marcadoresParking);
    }

    private void añadirParkingYReparacion() {
        final ArrayList<OverlayItem> items = new ArrayList<>();
        // Datos extraídos de los archivos GeoJSON (coordenadas, nombre, dirección): https://www.vitoria-gasteiz.org/geovitoria/geo?idioma=ES#YWNjaW9uPXNob3cmaWQ9MjE1MTAmbj11bmRlZmluZWQ=
        items.add(new OverlayItem(
                "Punto de reparación y mantenimiento de bicicletas",
                "FERMIN LASUEN, 1",
                new GeoPoint(42.855289, -2.670029)
        ));

        items.add(new OverlayItem(
                "Punto de reparación y mantenimiento de bicicletas",
                "FRANCISCO JAVIER DE LANDABURU",
                new GeoPoint(42.862555, -2.683456)
        ));

        items.add(new OverlayItem(
                "Oroimenezko bizikleten aparkaleku segurua",
                "OLAGUIBEL",
                new GeoPoint(42.846453, -2.671215)
        ));


        // Configurar iconos personalizados
        Drawable repairIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_bike_repar, null);
        Drawable parkingIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_parking_bici, null);

        Bitmap repairBitmap = ((BitmapDrawable) repairIcon).getBitmap();
        Bitmap scaledRepairBitmap = Bitmap.createScaledBitmap(repairBitmap, 32, 32, false);
        Drawable scaledRepairIcon = new BitmapDrawable(getResources(), scaledRepairBitmap);

        Bitmap parkingBitmap = ((BitmapDrawable) parkingIcon).getBitmap();
        Bitmap scaledParkingBitmap = Bitmap.createScaledBitmap(parkingBitmap, 32, 32, false);
        Drawable scaledParkingIcon = new BitmapDrawable(getResources(), scaledParkingBitmap);

        // Asignar iconos según el tipo de marcador
        for (OverlayItem item : items) {
            if (item.getTitle().contains("reparación")) {
                item.setMarker(scaledRepairIcon);
            } else {
                item.setMarker(scaledParkingIcon);
            }
        }
        // Crear el overlay con los items y el listener de clics
        ItemizedIconOverlay marcadoresParking = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        Toast.makeText(
                                BikeActivity.this,
                                item.getTitle(),
                                Toast.LENGTH_SHORT
                        ).show();
                        Toast.makeText(
                                BikeActivity.this,
                                item.getSnippet(),
                                Toast.LENGTH_SHORT
                        ).show();
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                },
                getApplicationContext()
        );

        // Añadir los marcadores al mapa
        map.getOverlays().add(marcadoresParking);
    }

    private void añadirMarkerInicial() {
        if (ubicacionAleatoria == null) return;
        Marker marker = new Marker(map);
        marker.setPosition(ubicacionAleatoria);
        marker.setTitle("Ubicación inicial");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_map_marker));
        map.getOverlays().add(marker);
    }

    private void cargarBidegorris() {
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

        kmlDocument = new KmlDocument();
        kmlDocument.parseGeoJSON(jsonString);

    }

    private GeoPoint generarUbicacionDentroVitoria() {
        bicisBB = bidegorrisPolygone.getBounds();
        double minLat = bicisBB.getLatSouth();
        double maxLat = bicisBB.getLatNorth();
        double minLon = bicisBB.getLonWest();
        double maxLon = bicisBB.getLonEast();

        double randomLat = minLat + Math.random() * (maxLat - minLat);
        double randomLon = minLon + Math.random() * (maxLon - minLon);

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);


        GeoPoint currentLocation = locationOverlay.getMyLocation();

        // Si no estamos dentro de Vitoria se genera una ubicacion aleatoria
        if (currentLocation != null && bidegorrisPolygone.getActualPoints().contains(currentLocation)) {
            locationOverlay.enableMyLocation();
            locationOverlay.enableFollowLocation();
            locationOverlay.setDrawAccuracyEnabled(true);
            return currentLocation;
        } else {
            return new GeoPoint(randomLat, randomLon);
        }
    }

    private void inicializarMapa() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        mMyMapController = map.getController();

        mMyMapController.animateTo(ubicacionAleatoria);
        map.getController().setZoom(16.0);
        map.getController().setCenter(ubicacionAleatoria);
        mMyMapController = map.getController();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cargarBidegorris();
                ubicacionAleatoria = generarUbicacionDentroVitoria();
                inicializarMapa();
                añadirMarkerInicial();
                marcarCampus();
                añadirParkingYReparacion();
                añadirParkingsSeguros();
                dibujarBidegorris();
                calcularRuta();

            } else {
                Toast.makeText(this, "Permisos necesarios no concedidos", Toast.LENGTH_SHORT).show();
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


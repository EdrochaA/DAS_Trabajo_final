package com.example.unigo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;

import com.example.unigo.R;
import com.example.unigo.model.ParadaBus;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BusActivity extends AppCompatActivity {

    private static final int REQUEST_PERMS = 1;
    private static final String TAG = "BusPlanner";
    private FusedLocationProviderClient fusedLocationClient;

    private final List<ParadaBus> paradasUrbanas = new ArrayList<>();
    private final Map<String, List<Integer>> viajesUrbanos = new HashMap<>();
    private final List<ParadaBus> paradasInterurbanas = new ArrayList<>();
    private final Map<String, List<Integer>> viajesInterurbanos = new HashMap<>();

    private MapView map;
    private final GeoPoint campusAlava = new GeoPoint(42.8386, -2.6733);
    private GeoPoint ubicacionActual;
    private TextView tvInfo;
    private LinearLayout listaParadas;
    private ScrollView panelRuta;
    private ProgressBar progressBar;
    private boolean infoExpandida = false;
    private CardView cardPanelContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_bus);

        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkPermissionsAndStart();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);

        map.setOnTouchListener((v, event) -> {
            if (cardPanelContainer.getVisibility() == View.VISIBLE) {
                togglePanel();
                return true;
            }
            return false;
        });

        tvInfo = findViewById(R.id.tv_info);
        listaParadas = findViewById(R.id.lista_paradas);
        panelRuta = findViewById(R.id.panel_ruta);
        progressBar = findViewById(R.id.progressBar);
        cardPanelContainer = findViewById(R.id.card_panel_container);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_toggle_panel).setOnClickListener(v -> togglePanel());
    }

    private void togglePanel() {
        if (cardPanelContainer.getVisibility() == View.GONE) {
            cardPanelContainer.setVisibility(View.VISIBLE);
            cardPanelContainer.animate().translationX(0).setDuration(300).start();
        } else {
            cardPanelContainer.animate().translationX(cardPanelContainer.getWidth() + 50).setDuration(300)
                    .withEndAction(() -> cardPanelContainer.setVisibility(View.GONE)).start();
        }
    }

    private void setupInfoCard(String infoText) {
        tvInfo.setText(infoText);
        tvInfo.setMaxLines(3);
        infoExpandida = false;
        tvInfo.setOnClickListener(v -> {
            if (infoExpandida) {
                tvInfo.setMaxLines(3);
            } else {
                tvInfo.setMaxLines(Integer.MAX_VALUE);
            }
            infoExpandida = !infoExpandida;
        });
        tvInfo.setVisibility(View.VISIBLE);
    }

    private void checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMS);
        } else {
            obtenerUbicacionEIniciarLogica();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionEIniciarLogica();
        } else {
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionEIniciarLogica() {
        Toast.makeText(this, getString(R.string.getting_location_route), Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(this, location -> {
            if (location != null) {
                this.ubicacionActual = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Ubicación obtenida: " + ubicacionActual.toString());
                map.getController().setZoom(14.5);
                map.getController().setCenter(ubicacionActual);
                marcarUbicacionActual();
                marcarCampusAlava();
                new Thread(this::planificarViaje).start();
            } else {
                Toast.makeText(this, getString(R.string.location_not_found_gps), Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void planificarViaje() {
        Log.d(TAG, "Iniciando planificación de viaje...");
        cargarDatosDeTransporte();
        Log.d(TAG, "Datos cargados. Paradas urbanas: " + paradasUrbanas.size() + ", Paradas interurbanas: " + paradasInterurbanas.size());

        if (paradasUrbanas.isEmpty() || paradasInterurbanas.isEmpty()) {
            Log.e(TAG, "Una de las listas de paradas está vacía. Abortando.");
            runOnUiThread(() -> {
                setupInfoCard(getString(R.string.error_loading_stops));
                progressBar.setVisibility(View.GONE);
            });
            return;
        }

        ParadaBus paradaInicialUrbana = encontrarParadaMasCercana(ubicacionActual, paradasUrbanas);
        ParadaBus paradaInicialInterurbana = encontrarParadaMasCercana(ubicacionActual, paradasInterurbanas);

        if (paradaInicialUrbana == null || paradaInicialInterurbana == null) {
            runOnUiThread(() -> {
                setupInfoCard(getString(R.string.no_stops_nearby));
                progressBar.setVisibility(View.GONE);
            });
            return;
        }

        double distUrbana = paradaInicialUrbana.getGeoPoint().distanceToAsDouble(ubicacionActual);
        double distInterurbana = paradaInicialInterurbana.getGeoPoint().distanceToAsDouble(ubicacionActual);

        Log.d(TAG, "Parada urbana más cercana: " + paradaInicialUrbana.getStopName() + " a " + distUrbana + " metros.");
        Log.d(TAG, "Parada interurbana más cercana: " + paradaInicialInterurbana.getStopName() + " a " + distInterurbana + " metros.");

        if (distUrbana <= distInterurbana) {
            Log.d(TAG, "Decisión: Planificando VIAJE URBANO.");
            planificarViajeUrbano(paradaInicialUrbana);
        } else {
            Log.d(TAG, "Decisión: Planificando VIAJE INTERURBANO.");
            planificarViajeInterurbano();
        }
    }

    private void planificarViajeUrbano(ParadaBus paradaInicial) {
        Log.d(TAG, "Buscando ruta urbana desde: " + paradaInicial.getStopName());
        ParadaBus paradaFinal = encontrarParadaMasCercana(campusAlava, paradasUrbanas);
        Log.d(TAG, "Parada urbana final más cercana al campus: " + paradaFinal.getStopName());

        List<Integer> rutaBus = buscarRutaDirecta(paradaInicial.getStopId(), paradaFinal.getStopId(), viajesUrbanos);

        if (rutaBus == null) {
            Log.w(TAG, "No se encontró ruta URBANA directa.");
            runOnUiThread(() -> {
                setupInfoCard(getString(R.string.error_no_direct_urban_route));
                progressBar.setVisibility(View.GONE);
            });
            return;
        }

        Log.d(TAG, "Ruta URBANA directa encontrada. Dibujando...");
        runOnUiThread(() -> {
            String info = getString(R.string.info_urban_route_format, paradaInicial.getStopName(), paradaFinal.getStopName());
            setupInfoCard(info);
            mostrarRutaCompleta(ubicacionActual, paradaInicial, getParadasDesdeIds(rutaBus, paradasUrbanas), paradaFinal, campusAlava);
            progressBar.setVisibility(View.GONE);
        });
    }

    private void planificarViajeInterurbano() {
        Log.d(TAG, "Planificando viaje interurbano desde el HUB de Bilbao.");
        ParadaBus paradaInicial = getParadaPorId(17, paradasInterurbanas);
        ParadaBus paradaIntermedia = getParadaPorId(18, paradasInterurbanas);

        if (paradaInicial == null || paradaIntermedia == null) {
            String errorMsg = getString(R.string.error_critical_data_hubs);
            Log.e(TAG, errorMsg);
            runOnUiThread(() -> {
                setupInfoCard(errorMsg);
                progressBar.setVisibility(View.GONE);
            });
            return;
        }

        Log.d(TAG, "Buscando bus desde '" + paradaInicial.getStopName() + "' hasta '" + paradaIntermedia.getStopName() + "'");
        List<Integer> rutaInterurbana = buscarRutaDirecta(paradaInicial.getStopId(), paradaIntermedia.getStopId(), viajesInterurbanos);

        if (rutaInterurbana == null) {
            Log.w(TAG, "No se encontró ruta INTERURBANA directa entre la estación de Bilbao y la de Vitoria.");
            runOnUiThread(() -> {
                setupInfoCard(getString(R.string.error_no_interurban_route));
                progressBar.setVisibility(View.GONE);
            });
            return;
        }

        Log.d(TAG, "Ruta INTERURBANA encontrada. Buscando tramo urbano...");
        ParadaBus paradaUrbanaInicial = encontrarParadaMasCercana(paradaIntermedia.getGeoPoint(), paradasUrbanas);
        ParadaBus paradaUrbanaFinal = encontrarParadaMasCercana(campusAlava, paradasUrbanas);

        if (paradaUrbanaInicial == null || paradaUrbanaFinal == null) {
            Log.e(TAG, "No se pudo encontrar una parada urbana cercana a la estación o al campus.");
            runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            return;
        }

        Log.d(TAG, "Buscando bus URBANO desde '" + paradaUrbanaInicial.getStopName() + "' hasta '" + paradaUrbanaFinal.getStopName() + "'");
        List<Integer> rutaUrbana = buscarRutaDirecta(paradaUrbanaInicial.getStopId(), paradaUrbanaFinal.getStopId(), viajesUrbanos);

        if (rutaUrbana == null) {
            Log.w(TAG, "Ruta interurbana encontrada, pero NO se encontró ruta URBANA para el transbordo.");
            runOnUiThread(() -> {
                setupInfoCard(getString(R.string.info_interurban_walk_from_station));
                mostrarRutaCompleta(ubicacionActual, paradaInicial, getParadasDesdeIds(rutaInterurbana, paradasInterurbanas), paradaIntermedia, campusAlava);
                progressBar.setVisibility(View.GONE);
            });
            return;
        }

        Log.d(TAG, "Ruta de transbordo completa encontrada. Dibujando...");
        runOnUiThread(() -> {
            String info = getString(R.string.info_transfer_route_format,
                    paradaInicial.getStopName(), paradaUrbanaInicial.getStopName(), paradaUrbanaFinal.getStopName());
            setupInfoCard(info);
            mostrarRutaCompleta(ubicacionActual, paradaInicial, getParadasDesdeIds(rutaInterurbana, paradasInterurbanas), paradaIntermedia, null);
            mostrarRutaCompleta(paradaIntermedia.getGeoPoint(), paradaUrbanaInicial, getParadasDesdeIds(rutaUrbana, paradasUrbanas), paradaUrbanaFinal, campusAlava);
            progressBar.setVisibility(View.GONE);
        });
    }

    private void cargarDatosDeTransporte() {
        cargarDatosUrbanos();
        cargarDatosInterurbanos();
    }

    private void cargarDatosUrbanos() {
        paradasUrbanas.clear();
        viajesUrbanos.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stops.txt")))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 7) {
                    try {
                        String stopName = p[2].replace("\"", "").trim();
                        paradasUrbanas.add(new ParadaBus(Integer.parseInt(p[0]), stopName, Double.parseDouble(p[5]), Double.parseDouble(p[6])));
                    } catch (NumberFormatException e) {
                        Log.e("BUS_PARSE_ERROR", "Error en stops.txt: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("BUS_ERROR", "Error leyendo stops.txt", e);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stop_times.txt")))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 4) {
                    viajesUrbanos.computeIfAbsent(p[0], k -> new ArrayList<>()).add(Integer.parseInt(p[3]));
                }
            }
        } catch (IOException e) {
            Log.e("BUS_ERROR", "Error leyendo stop_times.txt", e);
        }
    }

    private void cargarDatosInterurbanos() {
        paradasInterurbanas.clear();
        viajesInterurbanos.clear();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stopsUnion.txt")))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (p.length >= 6) {
                    try {
                        String stopName = p[2].replace("\"", "").trim();
                        paradasInterurbanas.add(new ParadaBus(Integer.parseInt(p[0]), stopName, Double.parseDouble(p[4]), Double.parseDouble(p[5])));
                    } catch (NumberFormatException e) {
                        Log.e("BUS_PARSE_ERROR", "Error en stopsUnion.txt: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("BUS_ERROR", "Error leyendo stopsUnion.txt", e);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stop_timesUnion.txt")))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 4) {
                    viajesInterurbanos.computeIfAbsent(p[0], k -> new ArrayList<>()).add(Integer.parseInt(p[3]));
                }
            }
        } catch (IOException e) {
            Log.e("BUS_ERROR", "Error leyendo stop_timesUnion.txt", e);
        }
    }

    private List<Integer> buscarRutaDirecta(int origenId, int destinoId, Map<String, List<Integer>> viajes) {
        for (List<Integer> paradasDelViaje : viajes.values()) {
            if (paradasDelViaje.contains(origenId) && paradasDelViaje.contains(destinoId)) {
                int indexOrigen = paradasDelViaje.indexOf(origenId);
                int indexDestino = paradasDelViaje.indexOf(destinoId);
                if (indexOrigen < indexDestino) {
                    return paradasDelViaje.subList(indexOrigen, indexDestino + 1);
                }
            }
        }
        return null;
    }

    private ParadaBus encontrarParadaMasCercana(GeoPoint punto, List<ParadaBus> listaDeParadas) {
        if(listaDeParadas == null || listaDeParadas.isEmpty()){
            return null;
        }
        ParadaBus masCercana = null;
        double distanciaMinima = Double.MAX_VALUE;
        for (ParadaBus parada : listaDeParadas) {
            double d = punto.distanceToAsDouble(parada.getGeoPoint());
            if (d < distanciaMinima) {
                distanciaMinima = d;
                masCercana = parada;
            }
        }
        return masCercana;
    }

    private void mostrarRutaCompleta(GeoPoint inicio, ParadaBus p1, List<ParadaBus> paradasBus, ParadaBus p2, GeoPoint fin) {
        if (inicio == null || p1 == null || p2 == null) return;
        drawWalkingRoute(inicio, p1.getGeoPoint());
        mostrarRutaBus(paradasBus);
        if (fin != null) {
            drawWalkingRoute(p2.getGeoPoint(), fin);
        }
        mostrarParada(p1, R.drawable.ic_parada);
        if (p1.getStopId() != p2.getStopId()) {
            mostrarParada(p2, R.drawable.ic_parada);
        }
    }

    private void mostrarRutaBus(List<ParadaBus> paradas) {
        if (paradas == null || paradas.isEmpty()) return;
        ArrayList<GeoPoint> puntos = new ArrayList<>();
        for (ParadaBus p : paradas) {
            puntos.add(p.getGeoPoint());
        }
        drawRoadRoute(puntos, Color.BLUE);
        listaParadas.removeViews(1, listaParadas.getChildCount() - 1);
        for (ParadaBus p : paradas) {
            TextView t = new TextView(this);
            t.setText("• " + p.getStopName());
            t.setTextSize(16f);
            t.setPadding(8, 8, 8, 8);
            listaParadas.addView(t);
        }
    }

    private void drawRoadRoute(ArrayList<GeoPoint> waypoints, int color) {
        new Thread(() -> {
            RoadManager roadManager = new OSRMRoadManager(this, "UnigoApp/1.0");
            if (color == Color.BLUE) {
                ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_CAR);
            } else {
                ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT);
            }
            Road road = roadManager.getRoad(waypoints);
            runOnUiThread(() -> {
                if (road.mStatus == Road.STATUS_OK) {
                    Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                    roadOverlay.getPaint().setColor(color);
                    roadOverlay.getPaint().setStrokeWidth(8f);
                    if (color == Color.RED) {
                        roadOverlay.getPaint().setPathEffect(new DashPathEffect(new float[]{15, 10}, 0));
                    }
                    map.getOverlays().add(roadOverlay);
                    map.invalidate();
                } else {
                    Log.e(TAG, "No se pudo obtener la ruta por carretera. Estado: " + road.mStatus);
                    Polyline fallbackLine = new Polyline();
                    fallbackLine.setPoints(waypoints);
                    fallbackLine.getPaint().setColor(color);
                    map.getOverlays().add(fallbackLine);
                    map.invalidate();
                }
            });
        }).start();
    }

    private void drawWalkingRoute(GeoPoint start, GeoPoint end) {
        drawRoadRoute(new ArrayList<>(Arrays.asList(start, end)), Color.RED);
    }

    private void marcarUbicacionActual() {
        Marker marker = new Marker(map);
        marker.setPosition(ubicacionActual);
        marker.setTitle(getString(R.string.your_location));
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_map_marker, getTheme()));
        map.getOverlays().add(marker);
    }

    private void marcarCampusAlava() {
        Marker marker = new Marker(map);
        marker.setPosition(campusAlava);
        marker.setTitle(getString(R.string.campus_upv_ehu));
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_campus_red, getTheme()));
        map.getOverlays().add(marker);
    }

    private void mostrarParada(ParadaBus parada, int iconId) {
        Marker marker = new Marker(map);
        marker.setPosition(parada.getGeoPoint());
        marker.setTitle(parada.getStopName());
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), iconId, getTheme()));
        map.getOverlays().add(marker);
    }

    private List<ParadaBus> getParadasDesdeIds(List<Integer> ids, List<ParadaBus> listaMaestra) {
        List<ParadaBus> resultado = new ArrayList<>();
        for (int id : ids) {
            for (ParadaBus p : listaMaestra) {
                if (p.getStopId() == id) {
                    resultado.add(p);
                    break;
                }
            }
        }
        return resultado;
    }

    private ParadaBus getParadaPorNombre(String nombre, List<ParadaBus> lista) {
        Log.d(TAG, "Iniciando búsqueda de parada por nombre: '" + nombre + "'");
        for (ParadaBus p : lista) {
            if (p.getStopName().trim().equalsIgnoreCase(nombre.trim())) {
                Log.d(TAG, "¡Coincidencia encontrada! -> " + p.getStopName());
                return p;
            }
        }
        Log.w(TAG, "Búsqueda finalizada. No se encontró ninguna coincidencia para: '" + nombre + "'");
        return null;
    }

    private ParadaBus getParadaPorId(int stopId, List<ParadaBus> lista) {
        for (ParadaBus p : lista) {
            if (p.getStopId() == stopId) {
                return p;
            }
        }
        return null;
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
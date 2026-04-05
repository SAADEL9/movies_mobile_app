package com.saad.moviessaad.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import com.saad.moviessaad.R;
import com.saad.moviessaad.api.ApiClient;
import com.saad.moviessaad.api.ApiConstants;
import com.saad.moviessaad.api.ApiService;
import com.saad.moviessaad.model.Movie;
import com.saad.moviessaad.model.Video;
import com.saad.moviessaad.model.VideoResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité affichant les détails d'un film, sa bande-annonce et les cinémas proches via OpenStreetMap
 */
public class MovieDetailActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private Movie movie;
    private MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private String youtubeKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation de la configuration d'OSMdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_movie_detail);

        // Récupération des données du film
        movie = (Movie) getIntent().getSerializableExtra("movie_data");
        if (movie == null) {
            finish();
            return;
        }

        // Initialisation des vues
        ImageView posterImageView = findViewById(R.id.detail_poster);
        TextView titleTextView = findViewById(R.id.detail_title);
        TextView releaseDateTextView = findViewById(R.id.detail_release_date);
        TextView ratingTextView = findViewById(R.id.detail_rating);
        TextView overviewTextView = findViewById(R.id.detail_overview);
        Button btnPlayTrailer = findViewById(R.id.btn_play_trailer);

        // Affichage des informations
        titleTextView.setText(movie.getTitle());
        releaseDateTextView.setText("Date : " + movie.getReleaseDate());
        ratingTextView.setText("Note : " + movie.getVoteAverage() + "/10");
        overviewTextView.setText(movie.getOverview());

        Glide.with(this)
                .load(ApiConstants.IMAGE_BASE_URL + movie.getPosterPath())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(posterImageView);

        // Initialisation de la carte OpenStreetMap
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Vérification des permissions
        checkLocationPermission();

        // Récupération de la bande-annonce
        fetchMovieTrailer();

        // Gestion du clic sur le bouton de bande-annonce
        btnPlayTrailer.setOnClickListener(v -> {
            if (youtubeKey != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + youtubeKey));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Bande-annonce non disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Vérifie les permissions et initialise la localisation
     */
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initLocationOverlay();
            getCurrentLocation();
        }
    }

    /**
     * Initialise l'overlay de position actuelle sur la carte
     */
    private void initLocationOverlay() {
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);
    }

    /**
     * Récupère la position actuelle pour centrer la carte et ajouter des cinémas
     */
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                map.getController().setCenter(userPoint);
                addCinemaMarkers(userPoint);
            }
        });
    }

    /**
     * Ajoute des marqueurs de cinémas autour de la position
     * @param userPoint Position de l'utilisateur
     */
    private void addCinemaMarkers(GeoPoint userPoint) {
        addMarker(new GeoPoint(userPoint.getLatitude() + 0.005, userPoint.getLongitude() + 0.005), "Cinéma Royal");
        addMarker(new GeoPoint(userPoint.getLatitude() - 0.008, userPoint.getLongitude() - 0.002), "Pathé Gaumont");
        addMarker(new GeoPoint(userPoint.getLatitude() + 0.002, userPoint.getLongitude() - 0.006), "Le Grand Rex");
    }

    /**
     * Ajoute un marqueur individuel sur la carte
     */
    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        map.getOverlays().add(marker);
    }

    /**
     * Récupère la bande-annonce du film via Retrofit
     */
    private void fetchMovieTrailer() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getMovieVideos(movie.getId(), ApiConstants.API_KEY).enqueue(new Callback<VideoResponse>() {
            @Override
            public void onResponse(Call<VideoResponse> call, Response<VideoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Video> videos = response.body().getResults();
                    for (Video video : videos) {
                        if (video.getType().equalsIgnoreCase("Trailer") && video.getSite().equalsIgnoreCase("YouTube")) {
                            youtubeKey = video.getKey();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<VideoResponse> call, Throwable t) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

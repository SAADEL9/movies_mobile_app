package com.saad.moviessaad.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.saad.moviessaad.R;
import com.saad.moviessaad.api.ApiClient;
import com.saad.moviessaad.api.ApiConstants;
import com.saad.moviessaad.api.ApiService;
import com.saad.moviessaad.data.SupabaseService;
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
import com.saad.moviessaad.adapter.CastAdapter;
import com.saad.moviessaad.api.SupabaseApiClient;
import com.saad.moviessaad.api.SupabaseDbService;
import com.saad.moviessaad.model.CastMember;
import com.saad.moviessaad.model.CreditsResponse;
import com.saad.moviessaad.model.CrewMember;
import com.saad.moviessaad.model.UserRating;
import com.saad.moviessaad.model.WatchedItem;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité affichant les détails d'un film, sa bande-annonce et les cinémas proches via OpenStreetMap
 */
public class MovieDetailActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private int movieId;
    private String movieTitle;
    private String posterPath;
    private String backdropPath;
    private String overview;
    private String releaseDate;
    private String mediaType;
    private double rating;
    private MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private String youtubeKey = null;
    private boolean isBookmarked;
    private boolean isWatched;
    private String userId;
    private String userToken;
    private ImageView btnHeart;
    private MaterialButton btnMarkWatched;
    private List<ImageView> ratingStars;
    private float userPersonalRating = 0;
    private SupabaseDbService supabaseDbService;
    private CastAdapter castAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation de la configuration d'OSMdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_movie_detail);
        SystemBarInsets.applyToRoot(findViewById(android.R.id.content));

        SessionManager sessionManager = new SessionManager();
        userId = sessionManager.getCurrentUserId();
        userToken = sessionManager.getToken();
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        supabaseDbService = SupabaseApiClient.getClient().create(SupabaseDbService.class);
        readIntentData();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        TextView titleTextView = findViewById(R.id.detail_title);
        TextView releaseDateTextView = findViewById(R.id.detail_release_date);
        TextView ratingTextView = findViewById(R.id.detail_rating);
        TextView overviewTextView = findViewById(R.id.detail_overview);
        MaterialButton btnPlayTrailer = findViewById(R.id.btn_play_trailer);
        btnHeart = findViewById(R.id.btn_heart);
        btnMarkWatched = findViewById(R.id.btn_mark_watched);

        setupRatingStars();
        setupCastRecyclerView();
        
        // Affichage des informations
        titleTextView.setText(movieTitle);
        releaseDateTextView.setText(getString(R.string.release_format, releaseDate));
        ratingTextView.setText(getString(R.string.rating_format, rating));
        overviewTextView.setText(overview);

        ImageView backdropImage = findViewById(R.id.detail_backdrop);
        Glide.with(this)
                .load(ApiConstants.IMAGE_BASE_URL + ((backdropPath != null && !backdropPath.isEmpty()) ? backdropPath : posterPath))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(backdropImage);

        // Initialisation de la carte OpenStreetMap
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();
        fetchMovieTrailer();
        fetchMovieCredits();

        btnPlayTrailer.setOnClickListener(v -> {
            if (youtubeKey != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + youtubeKey));
                startActivity(intent);
            } else {
                showSnack("Trailer unavailable", R.color.colorMuted);
            }
        });

        btnHeart.setOnClickListener(v -> toggleBookmark());
        btnMarkWatched.setOnClickListener(v -> toggleWatchedStatus());

        checkWatchlistStatus();
        fetchWatchedStatus();
        fetchUserRating();
    }

    private void setupCastRecyclerView() {
        RecyclerView recyclerCast = findViewById(R.id.recycler_cast);
        castAdapter = new CastAdapter(this);
        recyclerCast.setAdapter(castAdapter);
    }

    private void fetchMovieCredits() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCredits(movieId, ApiConstants.API_KEY).enqueue(new Callback<CreditsResponse>() {
            @Override
            public void onResponse(Call<CreditsResponse> call, Response<CreditsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateCreditsUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<CreditsResponse> call, Throwable t) {}
        });
    }

    private void updateCreditsUI(CreditsResponse credits) {
        // Update Director
        for (CrewMember member : credits.getCrew()) {
            if ("Director".equalsIgnoreCase(member.getJob())) {
                findViewById(R.id.director_container).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.director_name)).setText(member.getName());
                ImageView directorImage = findViewById(R.id.director_image);
                Glide.with(this)
                        .load("https://image.tmdb.org/t/p/w185" + member.getProfilePath())
                        .circleCrop()
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(directorImage);
                break;
            }
        }

        // Update Cast (first 10)
        List<CastMember> cast = credits.getCast();
        if (cast != null) {
            List<CastMember> topCast = cast.subList(0, Math.min(cast.size(), 10));
            castAdapter.setCastList(topCast);
        }
    }

    private void setupRatingStars() {
        ratingStars = new ArrayList<>();
        ratingStars.add(findViewById(R.id.star1));
        ratingStars.add(findViewById(R.id.star2));
        ratingStars.add(findViewById(R.id.star3));
        ratingStars.add(findViewById(R.id.star4));
        ratingStars.add(findViewById(R.id.star5));

        for (int i = 0; i < ratingStars.size(); i++) {
            final int starIndex = i + 1;
            ratingStars.get(i).setOnClickListener(v -> submitPersonalRating(starIndex));
        }
    }

    private void fetchUserRating() {
        supabaseDbService.getUserRating("Bearer " + userToken, SupabaseApiClient.SUPABASE_KEY, userId, "eq." + movieId).enqueue(new Callback<List<UserRating>>() {
            @Override
            public void onResponse(Call<List<UserRating>> call, Response<List<UserRating>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    userPersonalRating = response.body().get(0).getRating();
                    updateRatingStarsUI();
                }
            }
            @Override
            public void onFailure(Call<List<UserRating>> call, Throwable t) {}
        });
    }

    private void updateRatingStarsUI() {
        for (int i = 0; i < ratingStars.size(); i++) {
            if (i < userPersonalRating) {
                ratingStars.get(i).setColorFilter(getColor(R.color.rating_gold));
            } else {
                ratingStars.get(i).setColorFilter(getColor(R.color.rating_gray));
            }
        }
    }

    private void submitPersonalRating(int stars) {
        UserRating ratingObj = new UserRating(userId, movieId, stars);
        supabaseDbService.submitRating("Bearer " + userToken, SupabaseApiClient.SUPABASE_KEY, "resolution=merge-duplicates", ratingObj).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    userPersonalRating = stars;
                    updateRatingStarsUI();
                    showSnack("You rated this " + stars + " stars", R.color.colorSuccess);
                    
                    // Animate selected star
                    ratingStars.get(stars - 1).animate().scaleX(1.3f).scaleY(1.3f).setDuration(200).withEndAction(() -> 
                        ratingStars.get(stars - 1).animate().scaleX(1.0f).scaleY(1.0f).setDuration(200)
                    );
                } else {
                    showSnack("Failed to save rating", R.color.colorError);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showSnack("Connection error", R.color.colorError);
            }
        });
    }

    private void fetchWatchedStatus() {
        supabaseDbService.checkWatched("Bearer " + userToken, SupabaseApiClient.SUPABASE_KEY, userId, "eq." + movieId).enqueue(new Callback<List<WatchedItem>>() {
            @Override
            public void onResponse(Call<List<WatchedItem>> call, Response<List<WatchedItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isWatched = !response.body().isEmpty();
                    updateWatchedButton();
                }
            }
            @Override
            public void onFailure(Call<List<WatchedItem>> call, Throwable t) {}
        });
    }

    private void readIntentData() {
        Intent intent = getIntent();
        movieId = intent.getIntExtra("movie_id", -1);
        movieTitle = intent.getStringExtra("movie_title");
        posterPath = intent.getStringExtra("movie_poster");
        rating = intent.getDoubleExtra("movie_rating", 0.0);
        overview = intent.getStringExtra("movie_overview");
        releaseDate = intent.getStringExtra("movie_release");
        backdropPath = intent.getStringExtra("movie_backdrop");
        mediaType = intent.getStringExtra("media_type");
        if (movieTitle == null) movieTitle = "Unknown";
        if (posterPath == null) posterPath = "";
        if (overview == null || overview.isEmpty()) overview = "No overview available.";
        if (releaseDate == null) releaseDate = "N/A";
        if (mediaType == null) mediaType = "movie";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_movie_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_chat) {
            Intent intent = new Intent(this, AvatarChatActivity.class);
            intent.putExtra("mode", "movie");
            intent.putExtra("movie_title", movieTitle);
            intent.putExtra("movie_overview", overview);
            intent.putExtra("movie_year", releaseDate);
            intent.putExtra("movie_rating", String.valueOf(rating));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateHeartIcon() {
        if (btnHeart == null) return;
        btnHeart.setImageResource(isBookmarked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private void updateWatchedButton() {
        if (btnMarkWatched == null) return;
        if (isWatched) {
            btnMarkWatched.setText("Watched");
            btnMarkWatched.setIconResource(R.drawable.ic_eye_open);
            btnMarkWatched.setAlpha(0.6f);
        } else {
            btnMarkWatched.setText("Mark as Watched");
            btnMarkWatched.setIconResource(R.drawable.ic_eye_outline);
            btnMarkWatched.setAlpha(1.0f);
        }
    }

    private void checkWatchlistStatus() {
        SupabaseService.INSTANCE.isMovieInWatchlist(userId, movieId, new SupabaseService.WatchlistStatusCallback() {
            @Override
            public void onSuccess(boolean isInWatchlist, boolean watched) {
                isBookmarked = isInWatchlist;
                updateHeartIcon();
            }

            @Override
            public void onError(String message) {
                showSnack("Unable to sync watchlist", R.color.colorError);
            }
        });
    }

    private void toggleBookmark() {
        if (isBookmarked) {
            SupabaseService.INSTANCE.removeWatchlistItem(userId, movieId, new SupabaseService.ActionCallback() {
                @Override
                public void onSuccess() {
                    isBookmarked = false;
                    updateHeartIcon();
                    showSnack("Removed from Watchlist", R.color.colorMuted);
                }

                @Override
                public void onError(String message) {
                    showSnack("Error: " + message, R.color.colorError);
                }
            });
            return;
        }
        SupabaseService.INSTANCE.upsertWatchlistItem(userId, movieId, movieTitle, posterPath, rating, new SupabaseService.ActionCallback() {
            @Override
            public void onSuccess() {
                isBookmarked = true;
                updateHeartIcon();
                showSnack("✓ Added to Watchlist", R.color.colorSuccess);
            }

            @Override
            public void onError(String message) {
                showSnack("Error: " + message, R.color.colorError);
            }
        });
    }

    private void toggleWatchedStatus() {
        if (isWatched) {
            supabaseDbService.removeWatched("Bearer " + userToken, SupabaseApiClient.SUPABASE_KEY, userId, "eq." + movieId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        isWatched = false;
                        updateWatchedButton();
                        showSnack("Removed from Watched", R.color.colorMuted);
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    showSnack("Error: " + t.getMessage(), R.color.colorError);
                }
            });
        } else {
            WatchedItem item = new WatchedItem(userId, movieId, movieTitle, posterPath, (float) rating);
            supabaseDbService.addWatched("Bearer " + userToken, SupabaseApiClient.SUPABASE_KEY, "return=minimal", item).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        isWatched = true;
                        updateWatchedButton();
                        showSnack("Marked as watched", R.color.colorSuccess);
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    showSnack("Error: " + t.getMessage(), R.color.colorError);
                }
            });
        }
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
        Call<VideoResponse> trailerCall = "tv".equals(mediaType)
                ? apiService.getSeriesVideos(movieId, ApiConstants.API_KEY)
                : apiService.getMovieVideos(movieId, ApiConstants.API_KEY);
        trailerCall.enqueue(new Callback<VideoResponse>() {
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

    private void showSnack(String message, int actionColorRes) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getColor(R.color.colorSurface));
        snackbar.setTextColor(getColor(R.color.colorTextPrimary));
        snackbar.setActionTextColor(getColor(actionColorRes));
        snackbar.show();
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
                showSnack("Location permission denied", R.color.colorError);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}

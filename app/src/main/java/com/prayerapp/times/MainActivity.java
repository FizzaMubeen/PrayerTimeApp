package com.prayerapp.times;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.CurrentLocationRequest;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 200;

    private FusedLocationProviderClient fusedLocationClient;

    private TextView locationText, dateText;
    private TextView fajrTime, sunriseTime, dhuhrTime, asrTime, maghribTime, ishaTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationText = findViewById(R.id.locationText);
        dateText = findViewById(R.id.dateText);
        fajrTime = findViewById(R.id.fajrTime);
        sunriseTime = findViewById(R.id.sunriseTime);
        dhuhrTime = findViewById(R.id.dhuhrTime);
        asrTime = findViewById(R.id.asrTime);
        maghribTime = findViewById(R.id.maghribTime);
        ishaTime = findViewById(R.id.ishaTime);

        findViewById(R.id.refreshButton).setOnClickListener(v -> requestLocationAndCalculate());

        dateText.setText(android.text.format.DateFormat.format("EEEE, MMMM d, yyyy", Calendar.getInstance()));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationAndCalculate();
    }

    private void requestLocationAndCalculate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        fetchLocation();
    }

    @SuppressWarnings("MissingPermission")
    private void fetchLocation() {
        locationText.setText("Locating…");

        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationClient.getCurrentLocation(request, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        onLocationReady(location);
                    } else {
                        Toast.makeText(this, "Couldn't get location. Make sure GPS is on.", Toast.LENGTH_LONG).show();
                        locationText.setText("Location unavailable");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void onLocationReady(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        TimeZone tz = TimeZone.getDefault();
        double tzOffsetHours = tz.getOffset(System.currentTimeMillis()) / 3600000.0;

        PrayerTimeCalculator.Times times = PrayerTimeCalculator.calculate(lat, lon, Calendar.getInstance(), tzOffsetHours);

        fajrTime.setText(PrayerTimeCalculator.formatTime(times.fajr));
        sunriseTime.setText(PrayerTimeCalculator.formatTime(times.sunrise));
        dhuhrTime.setText(PrayerTimeCalculator.formatTime(times.dhuhr));
        asrTime.setText(PrayerTimeCalculator.formatTime(times.asr));
        maghribTime.setText(PrayerTimeCalculator.formatTime(times.maghrib));
        ishaTime.setText(PrayerTimeCalculator.formatTime(times.isha));

        resolveCityName(lat, lon);
    }

    private void resolveCityName(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> results = geocoder.getFromLocation(lat, lon, 1);
            if (results != null && !results.isEmpty()) {
                Address a = results.get(0);
                String city = a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
                String country = a.getCountryName();
                StringBuilder sb = new StringBuilder();
                if (city != null) sb.append(city);
                if (country != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(country);
                }
                locationText.setText(sb.length() > 0 ? sb.toString()
                        : String.format(Locale.US, "%.3f, %.3f", lat, lon));
            } else {
                locationText.setText(String.format(Locale.US, "%.3f, %.3f", lat, lon));
            }
        } catch (Exception e) {
            locationText.setText(String.format(Locale.US, "%.3f, %.3f", lat, lon));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission is needed to calculate prayer times for your area.", Toast.LENGTH_LONG).show();
                locationText.setText("Location permission denied");
            }
        }
    }
}

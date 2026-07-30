package com.prayerapp.times;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches official prayer times from the Aladhan API (https://aladhan.com),
 * using the Diyanet İşleri Başkanlığı (Turkey) calculation method — the
 * authority most commonly followed in Germany — which includes a properly
 * tested high-latitude adjustment (important for cities like Berlin where a
 * plain angle-based formula breaks down in summer).
 */
public class AladhanApiClient {

    private static final int METHOD_DIYANET = 13;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(Times times);
        void onFailure(Exception e);
    }

    public static class Times {
        public String fajr, sunrise, dhuhr, asr, maghrib, isha;
    }

    public static void fetchTimings(double latitude, double longitude, Callback callback) {
        executor.execute(() -> {
            try {
                long unixTimestamp = System.currentTimeMillis() / 1000L;
                String urlStr = "https://api.aladhan.com/v1/timings/" + unixTimestamp
                        + "?latitude=" + latitude
                        + "&longitude=" + longitude
                        + "&method=" + METHOD_DIYANET;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                conn.disconnect();

                JSONObject root = new JSONObject(response.toString());
                JSONObject data = root.getJSONObject("data");
                JSONObject timings = data.getJSONObject("timings");

                Times t = new Times();
                t.fajr = clean(timings.getString("Fajr"));
                t.sunrise = clean(timings.getString("Sunrise"));
                t.dhuhr = clean(timings.getString("Dhuhr"));
                t.asr = clean(timings.getString("Asr"));
                t.maghrib = clean(timings.getString("Maghrib"));
                t.isha = clean(timings.getString("Isha"));

                mainHandler.post(() -> callback.onSuccess(t));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        });
    }

    /** Strips the " (CEST)"-style timezone suffix and converts "HH:mm" (24h) to "H:MM AM/PM". */
    private static String clean(String raw) {
        String hhmm = raw.split(" ")[0]; // drop " (CEST)" etc.
        String[] parts = hhmm.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%d:%02d %s", displayHour, minute, period);
    }
}

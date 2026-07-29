# Prayer Times — Native Android App

A standalone, personal Android app that calculates the five daily prayer
times (plus sunrise) directly from your phone's GPS location — no website,
no external service, no third-party app branding.

## How it works

- Gets your current GPS coordinates (`FusedLocationProviderClient`).
- Runs a solar-position calculation (`PrayerTimeCalculator.java`) using your
  latitude, longitude, date, and timezone to work out Fajr, Sunrise, Dhuhr,
  Asr, Maghrib and Isha — the same underlying astronomy every prayer-time
  app uses.
- Shows your city name (reverse-geocoded) and today's date.
- "Refresh Location" re-fetches your GPS position and recalculates.

**Calculation method used:** Muslim World League angles (Fajr 18°, Isha 17°,
Asr = Shafi'i/standard shadow factor). If you follow a different method
(ISNA, Umm al-Qura, Hanafi Asr, etc.), tell me and I'll adjust the constants
at the top of `PrayerTimeCalculator.java` — it's a one-line change per value.

## Build the APK via GitHub (no software installed on your computer)

1. Unzip this folder.
2. Create a free account at [github.com](https://github.com) if you don't have one.
3. Click **+** → **New repository**, give it any name, keep it Public or
   Private, and don't add a README/gitignore when prompted.
4. On the empty repo page, click **"uploading an existing file"**.
5. Drag in **everything** from this folder, including the hidden `.github`
   folder (enable "show hidden items" in your file browser if you don't see it).
6. Click **Commit changes**.
7. Go to the **Actions** tab — a "Build APK" run starts automatically and
   finishes in about 2–3 minutes.
8. Click the finished run → scroll to **Artifacts** → download
   `prayer-times-debug-apk` (a zip containing your `.apk`).
9. Send that `.apk` to your phone and tap to install (allow "install unknown
   apps" when prompted — this is normal for apps installed outside the Play Store).

## Permissions the app asks for

- **Location** — required, to calculate prayer times for wherever you are.
- **Internet** — only used for reverse-geocoding your GPS coordinates into a
  city name for display; the actual prayer time math works fully offline.

## Notes

- First launch will prompt for location permission — accept it, then times
  populate automatically.
- If GPS is off or you're indoors with poor signal, tap "Refresh Location"
  once you have a clearer view of the sky, or turn on Wi-Fi (helps Android's
  location accuracy indoors).

package com.prayerapp.times;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Calculates the five daily prayer times plus sunrise, based on the sun's
 * position for a given date, latitude, longitude and timezone.
 *
 * Method used: Muslim World League
 *   Fajr angle   = 18 degrees below horizon
 *   Isha angle   = 17 degrees below horizon
 *   Asr          = Shafi'i (shadow factor 1)
 *
 * These are the same standard solar-angle conventions used by most prayer
 * time apps worldwide; you can tweak the constants below if you prefer a
 * different school/method (e.g. ISNA uses 15/15, Umm al-Qura uses 18.5/90min).
 */
public class PrayerTimeCalculator {

    private static final double FAJR_ANGLE = 18.0;
    private static final double ISHA_ANGLE = 17.0;
    private static final double ASR_SHADOW_FACTOR = 1.0; // 1 = Shafi'i/Maliki/Hanbali, 2 = Hanafi

    public static class Times {
        public double fajr, sunrise, dhuhr, asr, maghrib, isha;
    }

    public static Times calculate(double latitude, double longitude, Calendar date, double timezoneOffsetHours) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);

        double jd = julianDate(year, month, day) - longitude / (15.0 * 24.0);

        Times t = new Times();
        t.fajr = computeTime(jd, latitude, longitude, timezoneOffsetHours, -FAJR_ANGLE, true);
        t.sunrise = computeTime(jd, latitude, longitude, timezoneOffsetHours, -0.833, true);
        t.dhuhr = computeMidday(jd, longitude, timezoneOffsetHours) + 2.0 / 60.0; // small safety margin
        t.asr = computeAsr(jd, latitude, longitude, timezoneOffsetHours, ASR_SHADOW_FACTOR);
        t.maghrib = computeTime(jd, latitude, longitude, timezoneOffsetHours, -0.833, false);
        t.isha = computeTime(jd, latitude, longitude, timezoneOffsetHours, -ISHA_ANGLE, false);
        return t;
    }

    // ---- Core solar math ----

    private static double julianDate(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    private static double sunDeclination(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        return darcsin(dsin(e) * dsin(l));
    }

    private static double equationOfTime(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0;
        return q / 15.0 - fixHour(ra);
    }

    private static double computeMidday(double jd, double longitude, double tz) {
        double eqt = equationOfTime(jd);
        return fixHour(12 - eqt) + tz - longitude / 15.0 + longitude / 15.0;
        // (longitude correction cancels because jd already shifted by longitude/360)
    }

    private static double computeTime(double jd, double lat, double lon, double tz, double angle, boolean isMorning) {
        double decl = sunDeclination(jd);
        double noon = computeMidday(jd, lon, tz);
        double t = (1.0 / 15.0) * darccos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat)));
        return isMorning ? noon - t : noon + t;
    }

    private static double computeAsr(double jd, double lat, double lon, double tz, double shadowFactor) {
        double decl = sunDeclination(jd);
        double noon = computeMidday(jd, lon, tz);
        double angle = -darccot(shadowFactor + dtan(Math.abs(lat - decl)));
        double t = (1.0 / 15.0) * darccos((dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat)));
        return noon + t;
    }

    // ---- trig helpers (degrees) ----
    private static double dsin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double dcos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double dtan(double d) { return Math.tan(Math.toRadians(d)); }
    private static double darcsin(double x) { return Math.toDegrees(Math.asin(x)); }
    private static double darccos(double x) { return Math.toDegrees(Math.acos(x)); }
    private static double darctan2(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
    private static double darccot(double x) { return Math.toDegrees(Math.atan2(1.0, x)); }
    private static double fixAngle(double a) { return a - 360.0 * Math.floor(a / 360.0); }
    private static double fixHour(double h) { return h - 24.0 * Math.floor(h / 24.0); }

    /** Converts a decimal-hour value (e.g. 5.5) into a "HH:MM" 12-hour string. */
    public static String formatTime(double decimalHours) {
        decimalHours = fixHour(decimalHours);
        int hours = (int) decimalHours;
        int minutes = (int) Math.round((decimalHours - hours) * 60);
        if (minutes == 60) {
            minutes = 0;
            hours = (hours + 1) % 24;
        }
        String period = hours >= 12 ? "PM" : "AM";
        int displayHour = hours % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%d:%02d %s", displayHour, minutes, period);
    }
}

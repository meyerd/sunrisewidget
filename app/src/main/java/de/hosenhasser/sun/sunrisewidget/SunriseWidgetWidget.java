package de.hosenhasser.sun.sunrisewidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Implementation of App Widget functionality.
 */
public class SunriseWidgetWidget extends AppWidgetProvider {
    private static final String TAG = "SunriseWidget";

    private PendingIntent service = null;

    private static final Criteria sLocationCriteria;

    static {
        sLocationCriteria = new Criteria();
        sLocationCriteria.setPowerRequirement(Criteria.POWER_LOW);
        sLocationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        sLocationCriteria.setCostAllowed(false);
    }

    private static final long STALE_LOCATION_NANOS = 10l * 60000000000l; // 10 minutes
    private boolean mOneTimeLocationListenerActive = false;
    private MyLocationListener mOneTimeLocationListener = null;

    Location mLastLocation = new Location("0.0", "0.0");

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final Calendar TIME = Calendar.getInstance();
        TIME.set(Calendar.MINUTE, 0);
        TIME.set(Calendar.SECOND, 0);
        TIME.set(Calendar.MILLISECOND, 0);

        final Intent in = new Intent(context, UpdateService.class);

        if (service == null)
        {
            service = PendingIntent.getService(context, 0, in, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), AlarmManager.INTERVAL_HALF_DAY, service);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
        if(SP.getBoolean("pref_auto_location", true)) {
            Log.d(TAG, "sunrise onUpdateData: " + sLocationCriteria.toString());
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            String provider = lm.getBestProvider(sLocationCriteria, true);
            if (TextUtils.isEmpty(provider)) {
                Log.d(TAG, "No available location providers matching criteria." + lm.getAllProviders());
                return;
            }

            final android.location.Location lastLocation = lm.getLastKnownLocation(provider);

            if (lastLocation == null ||
                    (SystemClock.elapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos())
                            >= STALE_LOCATION_NANOS) {
                Log.d(TAG, "Stale or missing last-known location; requesting single coarse location "
                        + "update.");
                disableOneTimeLocationListener(context);
                mOneTimeLocationListenerActive = true;
                mOneTimeLocationListener = new MyLocationListener(context);
                lm.requestSingleUpdate(provider, mOneTimeLocationListener, null);
            } else {
                mLastLocation = new Location(lastLocation.getLatitude(), lastLocation.getLongitude());
            }
        }

        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, SettingsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sunrise_widget_widget);
            views.setOnClickPendingIntent(R.id.appwidget_widget_id, pendingIntent);

            updateAppWidget(context, appWidgetManager, appWidgetIds[i], mLastLocation);
        }
    }

    private void disableOneTimeLocationListener(Context context) {
        if (mOneTimeLocationListenerActive) {
            LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
            lm.removeUpdates(mOneTimeLocationListener);
            mOneTimeLocationListenerActive = false;
        }
    }

    private static long getCurrentTimestamp() {
        return Calendar.getInstance().getTimeInMillis();
    }

    class MyLocationListener implements LocationListener {
        Context c;

        public MyLocationListener(Context context) {
            this.c = context;
        }

        @Override
        public void onLocationChanged(android.location.Location location) {
//            publishUpdate(location);
            mLastLocation = new Location(location.getLatitude(), location.getLongitude());
            disableOneTimeLocationListener(this.c);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        m.cancel(service);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, Location loc) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sunrise_widget_widget);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
        String lat = "0.0";
        String lon = "0.0";
        if(SP.getBoolean("pref_auto_location", true)) {
            lat = loc.getLatitude().toString();
            lon = loc.getLongitude().toString();
        } else {
            lat = SP.getString("pref_latitude", "48.14");
            lon = SP.getString("pref_longitude", "11.6");
        }
        Log.d(TAG, "Latitude: " + lat);
        Log.d(TAG, "Longitude: " + lon);
        Location location = new Location(lat, lon);
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault().getID());

        Log.d(TAG, "TimeZone: " + TimeZone.getDefault().getID());

        Calendar now = Calendar.getInstance();
        Calendar sunset = calculator.getOfficialSunsetCalendarForDate(now);

        // when time is after sunset, use the next day
        if (now.after(sunset)) {
            now.add(Calendar.DATE, 1);
            sunset = calculator.getOfficialSunsetCalendarForDate(now);
        }

        Calendar sunrise = calculator.getOfficialSunriseCalendarForDate(now);

        boolean isBeforeSunset = now.before(sunset);
        boolean isBeforeSunrise = now.before(sunrise);

        String inFormat;
        if (DateFormat.is24HourFormat(context)) {
            inFormat = "HH:mm";
        } else {
            inFormat = "h:mm a";
        }

        CharSequence officialSunriseForDate = new SimpleDateFormat(inFormat)
                .format(sunrise.getTime());
        CharSequence officialSunsetForDate = new SimpleDateFormat(inFormat)
                .format(sunset.getTime());

        // views.setTextViewText(R.id.appwidget_text, widgetText);
        views.setTextViewText(R.id.appwidget_sunrise_text, officialSunriseForDate.toString());
        views.setTextViewText(R.id.appwidget_sunset_text, officialSunsetForDate.toString());

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


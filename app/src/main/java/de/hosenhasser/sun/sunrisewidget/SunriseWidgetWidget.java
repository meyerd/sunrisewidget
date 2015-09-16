package de.hosenhasser.sun.sunrisewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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

            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sunrise_widget_widget);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("SunriseWidget", "Latitude: " + SP.getString("pref_latitude", "48.14"));
        Log.d("SunriseWidget", "Longitude: " + SP.getString("pref_longitude", "11.6"));
        Location location = new Location(SP.getString("pref_latitude", "48.14"), SP.getString("pref_longitude", "11.6"));
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault().getID());

        Log.d("SunriseWidget", "TimeZone: " + TimeZone.getDefault().getID());

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


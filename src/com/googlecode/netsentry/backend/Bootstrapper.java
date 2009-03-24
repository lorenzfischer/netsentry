package com.googlecode.netsentry.backend;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.googlecode.netsentry.backend.scheduler.CronScheduler;

/**
 * This receiver will be called when the android system has finished booting. It
 * will execute some initialization code like:
 * 
 * <ul>
 * <li>Scheduling automatic reset of counters.</li>
 * <li>Setting an alarm using the {@link AlarmManager} that updates the
 * interfaces stats on a regular basis (very 30 minutes).</li>
 * </ul>
 * 
 * @author lorenz fischer
 */
public class Bootstrapper extends BroadcastReceiver {

    /** TAG for logging. */
    private static final String TAG = "ns.Bootstrapper";

    /** Standard projection for all the columns of an interface stats record. */
    private static final String[] PROJECTION = new String[] { InterfaceStatsColumns._ID, // 0
            InterfaceStatsColumns.INTERFACE_NAME, // 1
            InterfaceStatsColumns.RESET_CRON_EXPRESSION // 2
    };

    /**
     * In order to prevent multiple initialization of the application we use
     * this variable.
     */
    private static AtomicBoolean sInitialized = new AtomicBoolean(false);

    /**
     * We use this listener to restart automatic updates when the user changes
     * the shared preference for the update interval.
     */
    private static SharedPreferences.OnSharedPreferenceChangeListener sPreferenceListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bootstrapper.initializeSystem(context);
    }

    /**
     * This method initializes the system.
     * 
     * @param context
     *            the context to use for the initialization.
     * @param context
     * @return true if the application could be initialized, false if it already
     *         was initialized and therefore has not been initialized a second
     *         time.
     */
    public static boolean initializeSystem(Context context) {
        if (sInitialized.compareAndSet(false, true)) {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor;

            Log.d(TAG, "Initializing NetSentry..");

            // Set an alarm to invoke the updater on a regular basis
            scheduleAutomaticUpdates(context);

            // Make sure we restart the automatic update alarm when the
            // preference for it changes
            registerSharedPreferencesListener(context);

            // Start scheduled reset jobs
            cursor = resolver.query(InterfaceStatsProvider.CONTENT_URI, PROJECTION, null, null,
                    null);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    String cronExpression = cursor.getString(2);
                    if (cronExpression != null) {
                        // schedule job (this stops already scheduled jobs)
                        Uri itemUri = Uri.withAppendedPath(InterfaceStatsProvider.CONTENT_URI, Long
                                .toString(cursor.getLong(0)));
                        Intent resetterIntent = Resetter.createResetterIntent(itemUri);
                        CronScheduler.scheduleJob(context, resetterIntent, cronExpression);
                    }
                    cursor.moveToNext();
                }
                cursor.close();
            }

            return true;
        }

        return false;
    }

    /**
     * Schedules automatic updates of the byte counters cancelling all
     * previously scheduled jobs.
     * 
     * @param context
     *            the context to use when loading resources.
     */
    private static void scheduleAutomaticUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Log.d(TAG, "Starting automatic updates schedule..");
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), Configuration
                .getUpdateInterval(context), PendingIntent.getBroadcast(context, 0, new Intent(
                Updater.ACTION_UPDATE_COUNTERS), PendingIntent.FLAG_CANCEL_CURRENT));
    }

    /**
     * Setup a preferences listener that will:
     * <ul>
     * <li>Re-schedule automatic updates when the user changes the <i>update
     * interval</i> preference.</li>
     * <li>Reset the notification levels on all interfaces and the usage
     * notification currently active when the user changes the usage threshold
     * preferences.</li>
     * </ul>
     * 
     * @param context
     *            the context to use when loading resources.
     */
    private static void registerSharedPreferencesListener(final Context context) {
        if (sPreferenceListener == null) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            sPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                        String key) {
                    if (Configuration.PREFERENCE_UPDATE_INTERVAL.equals(key)) {
                        scheduleAutomaticUpdates(context);
                    } else if (Configuration.PREFERENCE_USAGE_THRESHOLD_MEDIUM.equals(key)
                            || Configuration.PREFERENCE_USAGE_THRESHOLD_HIGH.equals(key)) {
                        final NotificationManager notificationManager;
                        final ContentResolver resolver;
                        final Cursor cursor;

                        // clear notifications
                        notificationManager = (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancel(Configuration.NOTIFICATION_ID_USAGE);

                        // reset notification levels
                        resolver = context.getContentResolver();
                        cursor = resolver.query(InterfaceStatsProvider.CONTENT_URI, PROJECTION,
                                null, null, null);

                        if ((cursor != null) && (cursor.getCount() > 0)) {
                            cursor.moveToFirst();
                            while (!cursor.isAfterLast()) {
                                ContentValues values;
                                values = new ContentValues();
                                values.put(InterfaceStatsColumns.NOTIFICATION_LEVEL, Integer
                                        .valueOf(0));
                                resolver.update(InterfaceStatsProvider.CONTENT_URI, values,
                                        InterfaceStatsColumns._ID + "=" + cursor.getLong(0), null);

                                cursor.moveToNext();
                            }
                        }
                    }
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(sPreferenceListener);
        }
    }
}

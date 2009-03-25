/**
 *
 */
package com.googlecode.netsentry.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This interface provides constant values values and static methods for the
 * NetSentry application.
 * 
 * @author lorenz fischer
 */
public final class Configuration {

    /** This is the name of the update interval shared preference. */
    public final static String PREFERENCE_UPDATE_INTERVAL = "update_interval";

    /**
     * This is the preference name of the "medium usage level" shared
     * preference.
     */
    public final static String PREFERENCE_USAGE_THRESHOLD_MEDIUM = "usage_threshold_medium";

    /** This is the preference name of the "high usage level" shared preference. */
    public final static String PREFERENCE_USAGE_THRESHOLD_HIGH = "usage_threshold_high";

    /**
     * The default value for the maximum number of bytes that can be freely
     * transmitted over the operator's network.
     */
    public final static long DEFAULT_TRANSMISSION_LIMIT = 250 << 10 << 10; // 250mb

    /**
     * As soon as the percentage of already transmitted data vs. the
     * transmission limit reaches this high, the text color of the counters in
     * the list will change to the low warning level color and a notification
     * will be shown to the user.
     */
    public final static String DEFAULT_USAGE_THRESHOLD_MEDIUM = "75";

    /**
     * As soon as the percentage of already transmitted data vs. the
     * transmission limit reaches this high, the text color of the counters in
     * the list will change to the severe warning level color and a notification
     * will be shown to the user.
     */
    public final static String DEFAULT_USAGE_THRESHOLD_HIGH = "90";

    /**
     * When the user starts the traffic meter for the first time (or when the
     * device has been rebooted) an alarm will be scheduled with this interval
     * for the {@link Updater} to be invoked at a regular basis.
     */
    public final static String DEFAULT_UPDATE_INTERVAL = Long.toString(30 * 60 * 1000);

    /** The cron expression for "every day". */
    public final static String CRON_EVERY_DAY = "0 0 0 * * ? *";

    /** The cron expression for "every month". */
    public final static String CRON_EVERY_MONTH = "0 0 0 1 * ? *";

    /** Usage notifications will all have this id. */
    public static final int NOTIFICATION_ID_USAGE = 1;

    /**
     * @param context
     *            the context to use when loading the preferences.
     * 
     * @return the current preference value for the medium usage threshold.
     */
    public static int getUsageThresholdMedium(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(Configuration.PREFERENCE_USAGE_THRESHOLD_MEDIUM,
                Configuration.DEFAULT_USAGE_THRESHOLD_MEDIUM));
    }

    /**
     * @param context
     *            the context to use when loading the preferences.
     * 
     * @return the current preference value for the medium usage threshold.
     */
    public static int getUsageThresholdHigh(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString(Configuration.PREFERENCE_USAGE_THRESHOLD_HIGH,
                Configuration.DEFAULT_USAGE_THRESHOLD_HIGH));
    }

    /**
     * @param context
     *            the context to use when loading the preferences.
     * 
     * @return the current preference value for the update interval to use.
     */
    public static long getUpdateInterval(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Long.parseLong(prefs.getString(Configuration.PREFERENCE_UPDATE_INTERVAL,
                Configuration.DEFAULT_UPDATE_INTERVAL));
    }
}

/**
 *
 */
package com.googlecode.netsentry.backend;

/**
 * This class provides constant values and static methods for the NetSentry
 * application.
 * 
 * @author lorenz fischer
 */
public final class Configuration {

    /** This is the name of the update interval shared preference. */
    public static final String PREFERENCE_UPDATE_INTERVAL = "update_interval";

    /**
     * This is the preference name of the "medium usage level" shared
     * preference.
     */
    public static final String PREFERENCE_USAGE_THRESHOLD_MEDIUM = "usage_threshold_medium";

    /** This is the preference name of the "high usage level" shared preference. */
    public static final String PREFERENCE_USAGE_THRESHOLD_HIGH = "usage_threshold_high";
    
    
    public static final String PREFERENCE_DISCRIMINATOR_2G3G_SIM = "discriminator_2g3g_sim_operator";
    
    public static final String PREFERENCE_DISCRIMINATOR_2G3G_NETWORK = "discriminator_2g3g_network_operator";

    /**
     * The default value for the maximum number of bytes that can be freely
     * transmitted over the operator's network.
     */
    public static final long DEFAULT_TRANSMISSION_LIMIT = 250 << 10 << 10; // 250mb

    /**
     * As soon as the percentage of already transmitted data vs. the
     * transmission limit reaches this high, the text color of the counters in
     * the list will change to the low warning level color and a notification
     * will be shown to the user.
     */
    public static final String DEFAULT_USAGE_THRESHOLD_MEDIUM = "75";

    /**
     * As soon as the percentage of already transmitted data vs. the
     * transmission limit reaches this high, the text color of the counters in
     * the list will change to the severe warning level color and a notification
     * will be shown to the user.
     */
    public static final String DEFAULT_USAGE_THRESHOLD_HIGH = "90";

    /**
     * When the user starts the traffic meter for the first time (or when the
     * device has been rebooted) an alarm will be scheduled with this interval
     * for the {@link Updater} to be invoked at a regular basis.
     */
    public static final String DEFAULT_UPDATE_INTERVAL = Long.toString(30 * 60 * 1000);

    /** The cron expression for "every day". */
    public static final String CRON_EVERY_DAY = "0 0 0 * * ? *";

    /** The cron expression for "every week". */
    public static final String CRON_EVERY_WEEK = "0 0 0 ? * 1 *";

    /** The cron expression for "every month". */
    public static final String CRON_EVERY_MONTH = "0 0 0 1 * ? *";

    /** Usage notifications will all have this id. */
    public static final int NOTIFICATION_ID_USAGE = 1;

}

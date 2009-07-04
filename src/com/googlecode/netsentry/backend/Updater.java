/**
 * 
 */
package com.googlecode.netsentry.backend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.googlecode.netsentry.R;

/**
 * This class listens for various broadcast messages and issues updates to the
 * {@link InterfaceStatsProvider} with the current counter values from the
 * system.
 * 
 * @author lorenz fischer
 */
public class Updater extends BroadcastReceiver {

    // /** TAG for logging. */
    private static final String TAG = "ns.Updater";

    /**
     * For every interface name (String) we keep the discriminator (String)
     * which was used for the last update. If the discriminator changes, we
     * issue an update for the previous discriminator first.
     */
    private static Map<String, String> sDiscriminatorMap = new ConcurrentHashMap<String, String>();

    /**
     * Whenever some component wants to issue an update of the counters it can
     * broadcast an intent with this name and this receiver will listen for it.
     */
    public static final String ACTION_UPDATE_COUNTERS = "com.googlecode.netsentry.ACTION_UPDATE_COUNTERS";

    /**
     * This is a virtual file of the OS containing counter values for the
     * various network interfaces of the phone. As far as I'm concerned this is
     * not the nicest way to read the information about network traffic, but it
     */
    private static final String INTERFACE_FILE = "/proc/self/net/dev";

    /** Standard projection for all the columns of an interface stats record. */
    private static final String[] PROJECTION_STATS = new String[] { // 
    InterfaceStatsColumns._ID, // 0
            InterfaceStatsColumns.INTERFACE_NAME, // 1
            InterfaceStatsColumns.BYTES_RECEIVED, // 2
            InterfaceStatsColumns.BYTES_SENT, // 3
            InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, // 4
            InterfaceStatsColumns.INTERFACE_ALIAS, // 5
            InterfaceStatsColumns.NOTIFICATION_LEVEL // 6
    };

    /** Projection for the the values table. */
    private static final String[] PROJECTION_VALUES = new String[] { //
    InterfaceValuesColumns._ID, // 0
            InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM, // 1
            InterfaceValuesColumns.BYTES_SENT_SYSTEM // 2
    };

    /*
     * TODO maybe we could increase the speed by only grouping on the fields of
     * interest.
     * 
     * TODO try scanner and see if it's faster
     */
    /**
     * This pattern will be used to parse one line in the interface file.
     */
    private static final Pattern LINE_PATTERN = Pattern.compile("^" + // start
            "(.*?):" + // the device name (group = 1)
            "\\s*" + // blanks
            "([0-9]+)" + // 1st number (group = 2) -> bytes received
            "\\s+" + // blanks
            "([0-9]+)" + // 2nd number (group = 3) -> packets received
            "\\s+" + // blanks
            "([0-9]+)" + // 3rd number (group = 4)
            "\\s+" + // blanks
            "([0-9]+)" + // 4th number (group = 5)
            "\\s+" + // blanks
            "([0-9]+)" + // 5th number (group = 6)
            "\\s+" + // blanks
            "([0-9]+)" + // 6th number (group = 7)
            "\\s+" + // blanks
            "([0-9]+)" + // 7th number (group = 8)
            "\\s+" + // blanks
            "([0-9]+)" + // 8th number (group = 9)
            "\\s+" + // blanks
            "([0-9]+)" + // 9th number (group = 10) -> bytes sent
            "\\s+" + // blanks
            "([0-9]+)" + // 10th number (group = 11) -> packets sent
            "\\s+" + // blanks
            "([0-9]+)" + // 11th number (group = 12)
            "\\s+" + // blanks
            "([0-9]+)" + // 12th number (group = 13)
            "\\s+" + // blanks
            "([0-9]+)" + // 13th number (group = 14)
            "\\s+" + // blanks
            "([0-9]+)" + // 14th number (group = 15)
            "\\s+" + // blanks
            "([0-9]+)" + // 15th number (group = 16)
            "\\s+" + // blanks
            "([0-9]+)" + // 16th number (group = 17)
            "$"); // end of the line

    /**
     * Our data store were we keep all the information about interfaces. Since
     * we want to create as few objects as possible we will always use the same
     * instances of {@link InterfaceStats} for every update. This is also the
     * reason why this map has to be thread safe.
     */
    private static final Map<String, InterfaceStats> sInterfaceStatsMap = //
    new ConcurrentHashMap<String, InterfaceStats>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final ContentResolver resolver = context.getContentResolver();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int usageThresholdMedium = Integer.parseInt(prefs.getString(
                Configuration.PREFERENCE_USAGE_THRESHOLD_MEDIUM,
                Configuration.DEFAULT_USAGE_THRESHOLD_MEDIUM));
        final int usageThresholdHigh = Integer.parseInt(prefs.getString(
                Configuration.PREFERENCE_USAGE_THRESHOLD_HIGH,
                Configuration.DEFAULT_USAGE_THRESHOLD_HIGH));

        Log.d(TAG, "Updating InterfaceStatsProvider..");

        if (Updater.updateInterfaceStats(context)) {
            /*
             * If we have some changes, check if we need to send notifications
             */
            Cursor cursor = resolver.query(InterfaceStatsProvider.CONTENT_URI_STATS,
                    PROJECTION_STATS, null, null, null);

            if ((cursor != null) && (cursor.getCount() > 0)) {
                Notification notification = null;
                int notificationCreated = 0; // 0=no, 1=low, 2=high

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    long bytesLimit = cursor.getLong(4);

                    if (bytesLimit > 0) {
                        long bytesTotal;
                        long usage;
                        int oldNotificationLevel = cursor.getInt(6);
                        int newNotificationLevel = oldNotificationLevel;

                        bytesTotal = cursor.getLong(2) + cursor.getLong(3);
                        usage = Double.valueOf((1.0D * bytesTotal / bytesLimit) * 100).longValue();

                        // notify user as necessary
                        if (usage >= usageThresholdHigh && oldNotificationLevel < 2) {
                            newNotificationLevel = 2;
                            if (notificationCreated < 2) {
                                notification = new Notification(R.drawable.icon_notification, null,
                                        System.currentTimeMillis());
                                notificationCreated = 2;
                            }
                        } else if (usage >= usageThresholdMedium && oldNotificationLevel < 1) {
                            newNotificationLevel = 1;
                            if (notificationCreated < 1) {
                                notification = new Notification(R.drawable.icon_notification, null,
                                        System.currentTimeMillis());
                                notificationCreated = 1;
                            }
                        }

                        if (newNotificationLevel > oldNotificationLevel) {
                            // the current interface has reached a new
                            // notification level
                            ContentValues values;
                            values = new ContentValues();
                            values.put(InterfaceStatsColumns.NOTIFICATION_LEVEL, Integer
                                    .valueOf(newNotificationLevel));
                            resolver.update(InterfaceStatsProvider.CONTENT_URI_STATS, values,
                                    InterfaceStatsColumns._ID + "=" + cursor.getLong(0), null);
                        }
                    }

                    cursor.moveToNext();
                }

                // close cursor
                cursor.close();

                if (notificationCreated > 0) {
                    NotificationManager notificationManager;
                    Intent typeIntent;
                    PendingIntent contentIntent;

                    notificationManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);

                    /*
                     * create the intent we add to the notification so the user
                     * can view the list
                     */
                    typeIntent = new Intent(Intent.ACTION_VIEW);
                    typeIntent.setType(InterfaceStatsProvider.CONTENT_TYPE_STATS);
                    contentIntent = PendingIntent.getActivity(context, 0, typeIntent, 0);

                    if (notificationCreated == 2) {
                        notification.setLatestEventInfo(context, context
                                .getText(R.string.notification_usage_level_high_title), context
                                .getString(R.string.notification_usage_level_high,
                                        usageThresholdHigh), contentIntent);

                    } else if (notificationCreated == 1) {
                        notification.setLatestEventInfo(context, context
                                .getText(R.string.notification_usage_level_medium_title), context
                                .getString(R.string.notification_usage_level_medium,
                                        usageThresholdMedium), contentIntent);
                    }

                    notificationManager.cancel(Configuration.NOTIFICATION_ID_USAGE);
                    notificationManager.notify(Configuration.NOTIFICATION_ID_USAGE, notification);
                }
            }
        }
    }

    /**
     * This method issues an update of the interface stats database. It does not
     * execute the update itself, but merely loops over all the interfaces that
     * we previously could find a system entry for (the things in
     * sInterfaceStatsMap) and calls
     * {@link #updateInterface(Context, String, String, String, long, long)} for
     * each one of them.
     * 
     * This method will not send any notifications to the system.
     * 
     * @param the
     *            context we can use to retrieve resources.
     * @return true if anything was updated at all, false otherwise.
     * @see #updateInterface(Context, String, String, String, long, long)
     */
    public static boolean updateInterfaceStats(Context context) {
        boolean updateIssued = false;

        Updater.updateDataMap();

        for (InterfaceStats stats : sInterfaceStatsMap.values()) {
            final long bytesReceivedCurrent = stats.getBytesReceived();
            final long bytesSentCurrent = stats.getBytesSent();

            /*
             * We only want a record in the database if we have any non-zero
             * values for the given record.
             */
            if (bytesReceivedCurrent > 0 || bytesSentCurrent > 0) {
                final String interfaceName = stats.getInterfaceName();
                String discriminator = null;
                String interfaceAlias = null;

                // TODO HIGH PRIO make dependent on app property
                if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_3G)) {
                    final TelephonyManager manager = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);
                    final StringBuilder discriminatorBuilder = new StringBuilder();
                    final StringBuilder interfaceAliasBuilder = new StringBuilder();

                    // sim discriminator
                    discriminatorBuilder.append(manager.getSubscriberId());
                    interfaceAliasBuilder.append(manager.getSimOperatorName());

                    // network discriminator
                    if (manager.getNetworkOperator().length() > 0
                            && !manager.getNetworkOperator().equals(manager.getSimOperator())) {
                        discriminatorBuilder.append("_").append(manager.getNetworkOperator());
                        interfaceAliasBuilder.append(" ").append(manager.getNetworkOperatorName());
                        interfaceAliasBuilder.append(" (").append(manager.getNetworkCountryIso())
                                .append(")");
                    }

                    discriminator = discriminatorBuilder.toString();
                    interfaceAlias = interfaceAliasBuilder.toString();
                }

                /*
                 * If we are using discriminators for this interface, we use the
                 * discriminator that was computed the last time we were
                 * updating. In doing so, we make sure that the traffic that
                 * summed up so far, is added to the correct counter.
                 * 
                 * This forces also us to make sure that the counters will be
                 * updated whenever the discriminator value might have changed.
                 * For 3g interfaces for instance, this could happen when the
                 * android.intent.action.PHONE_STATE (=
                 * android.telephony.TelephonyManager
                 * #ACTION_PHONE_STATE_CHANGED) intent was broadcasted.
                 */
                if (discriminator != null) {
                    String lastDiscriminator = sDiscriminatorMap.get(interfaceName);
                    sDiscriminatorMap.put(interfaceName, discriminator);
                    discriminator = lastDiscriminator;
                }

                if (updateInterface(context, interfaceName, discriminator, interfaceAlias,
                        bytesReceivedCurrent, bytesSentCurrent)) {
                    updateIssued = true;
                }
            }
        }

        return updateIssued;
    }

    /**
     * This method does the actual update magic.
     * 
     * @param context
     *            the context to use when retrieving the current counter from
     *            the data provider.
     * @param interfaceName
     *            the name of the interface we want to issue the update for.
     * @param interfaceAlias
     *            the alias for the interface, if <code>null</code> a default
     *            value will be used (@see
     *            {@link #getAliasForName(Context, String)}
     * @param bytesReceivedCurrent
     *            the current bytes received count as it was retrieved from the
     *            linux system counter.
     * @param bytesSentCurrent
     *            the current bytes sent count as it was retrieved from the
     *            linux system counter.
     * @return <code>true</code> if an update was issues, <code>false</code> if
     *         no update was necessary.
     */
    private static boolean updateInterface(final Context context, final String interfaceName,
            final String discriminator, String interfaceAlias, final long bytesReceivedCurrent,
            final long bytesSentCurrent) {
        final ContentResolver resolver = context.getContentResolver();
        boolean updateIssued = false;
        StringBuilder whereClause;
        final Cursor statsCursor; // Stats values
        final Cursor valuesCursor; // System Values
        final long bytesReceivedLast;
        final long bytesSentLast;
        final ContentValues systemValues;

        
        /*
         * Here we handle the InterfaceValues entry. This entry keeps track of
         * what the values of the OS counters were when we read them the last time.
         */
        whereClause = new StringBuilder();
        whereClause.append(InterfaceValuesColumns.INTERFACE_NAME).append("='").append(interfaceName)
        .append("'");
        valuesCursor = resolver.query(InterfaceStatsProvider.CONTENT_URI_VALUES, PROJECTION_VALUES,
                whereClause.toString(), null, null);
        
        systemValues = new ContentValues();
        systemValues.put(InterfaceValuesColumns.INTERFACE_NAME, interfaceName);
        systemValues.put(InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM, Long
                .valueOf(bytesReceivedCurrent));
        systemValues.put(InterfaceValuesColumns.BYTES_SENT_SYSTEM, Long.valueOf(bytesSentCurrent));
        systemValues.put(InterfaceValuesColumns.LAST_UPDATE, System.currentTimeMillis());
        
        if ((valuesCursor == null) || (valuesCursor.getCount() == 0)) {
            
            // we've never seen system values for this interface
            bytesReceivedLast = 0;
            bytesSentLast = 0;
            resolver.insert(InterfaceStatsProvider.CONTENT_URI_VALUES, systemValues);
        } else { 
            // we found a record in the values table
            valuesCursor.moveToFirst();
            bytesReceivedLast = valuesCursor.getLong(1);
            bytesSentLast = valuesCursor.getLong(2);
            
            // store values into the table
            resolver.update(InterfaceStatsProvider.CONTENT_URI_VALUES, systemValues,
                    InterfaceValuesColumns._ID + "=" + valuesCursor.getLong(0), null);
            valuesCursor.close();
        }
      
       
        /*
         * Now we can handle the InterfaceStats entry, which is what the user cares about more ;-)
         */
        whereClause = new StringBuilder();
        whereClause.append(InterfaceStatsColumns.INTERFACE_NAME).append("='").append(interfaceName)
                .append("'");
        if (discriminator == null) {
            whereClause.append("and ").append(InterfaceStatsColumns.INTERFACE_DISCRIMINATOR)
                    .append(" IS NULL");
        } else {
            whereClause.append("and ").append(InterfaceStatsColumns.INTERFACE_DISCRIMINATOR)
                    .append("='").append(discriminator).append("'");
        }
        statsCursor = resolver.query(InterfaceStatsProvider.CONTENT_URI_STATS, PROJECTION_STATS,
                whereClause.toString(), null, null);
        if ((statsCursor == null) || (statsCursor.getCount() == 0)) {
            // no entry for the device exists
            ContentValues values;

            if (interfaceAlias == null) {
                interfaceAlias = getAliasForName(context, interfaceName);
            }

            values = new ContentValues();
            values.put(InterfaceStatsColumns.INTERFACE_NAME, interfaceName);
            values.put(InterfaceStatsColumns.INTERFACE_DISCRIMINATOR, discriminator);
            values.put(InterfaceStatsColumns.INTERFACE_ALIAS, interfaceAlias);
            values.put(InterfaceStatsColumns.BYTES_RECEIVED, Long.valueOf(bytesReceivedCurrent));
            values.put(InterfaceStatsColumns.BYTES_SENT, Long.valueOf(bytesSentCurrent));

            // put default transmission limit and default automatic
            // reset for 3g devices
            if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_3G)) {
                values.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT,
                        Configuration.DEFAULT_TRANSMISSION_LIMIT);
                values.put(InterfaceStatsColumns.RESET_CRON_EXPRESSION,
                        Configuration.CRON_EVERY_MONTH);
            }

            // store values into the table
            resolver.insert(InterfaceStatsProvider.CONTENT_URI_STATS, values);

            updateIssued = true;
        } else {
            /*
             * entry exists, so update its contents if the delta values are > 0
             */
            long bytesReceivedDelta = 0;
            long bytesSentDelta = 0;
            long bytesReceivedDb;
            long bytesSentDb;

            statsCursor.moveToFirst();

            bytesReceivedDb = statsCursor.getLong(2);
            bytesSentDb = statsCursor.getLong(3);

            // compute the deltas
            if (bytesReceivedLast <= bytesReceivedCurrent) {
                bytesReceivedDelta = bytesReceivedCurrent - bytesReceivedLast;
            } else {
                // This could happen if the counters have been reset for
                // some reason
                bytesReceivedDelta = bytesReceivedCurrent;
            }

            if (bytesSentLast <= bytesSentCurrent) {
                bytesSentDelta = bytesSentCurrent - bytesSentLast;
            } else {
                // This could happen if the counters have been reset for
                // some reason
                bytesSentDelta = bytesSentCurrent;
            }

            // don't update anything if deltas are zero (= no change)
            if (bytesReceivedDelta + bytesSentDelta != 0) {
                ContentValues values;

                values = new ContentValues();

                bytesReceivedDb += bytesReceivedDelta;
                bytesSentDb += bytesSentDelta;

                // there have been some changes, so update the data
                // provider
                Log.v(TAG, "Values for device " + interfaceName
                        + " changed! bytes received delta: " + bytesReceivedDelta
                        + " bytes sent delta:" + bytesSentDelta);

                values.put(InterfaceStatsColumns.BYTES_RECEIVED, Long.valueOf(bytesReceivedDb));
                values.put(InterfaceStatsColumns.BYTES_SENT, Long.valueOf(bytesSentDb));
                values.put(InterfaceStatsColumns.LAST_UPDATE, System.currentTimeMillis());

                // store values into the table
                resolver.update(InterfaceStatsProvider.CONTENT_URI_STATS, values,
                        InterfaceStatsColumns._ID + "=" + statsCursor.getLong(0), null);

                updateIssued = true;
            }

        }

        // close cursor if it was opened
        if (statsCursor != null) {
            statsCursor.close();
        }

        return updateIssued;
    }

    /**
     * Computes the default interface alias for a given interface name.
     * 
     * @param context
     *            the context to use when retrieving the alias from the resource
     *            bundle.
     * @param interfaceName
     *            the interface name to compute the alias for.
     * 
     * @return the default interface alias to use for the interface.
     */
    public static String getAliasForName(Context context, String interfaceName) {
        if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_WIFI)) {
            return context.getString(R.string.provider_interface_alias_wifi);
        } else if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_3G)) {
            return context.getString(R.string.provider_interface_alias_2g3g);
        } else if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_ETHERNET)) {
            return context.getString(R.string.provider_interface_alias_ethernet);
        }

        return interfaceName;
    }

    /**
     * Reads the network counters and updates the static information map
     * {@link #sInterfaceStatsMap}. After the execution of this method this map
     * will contain up-to-date information about byte counters of all the
     * networking interfaces available to the android system.
     */
    private static void updateDataMap() {
        FileReader fstream = null;
        BufferedReader in = null;
        ArrayList<String> lines = new ArrayList<String>();

        try {

            fstream = new FileReader(INTERFACE_FILE);

            if (fstream != null) {
                try {
                    in = new BufferedReader(fstream, 500);
                    String line;

                    while ((line = in.readLine()) != null) {
                        lines.add(line);
                    }

                    in.close();
                    fstream.close();
                } catch (IOException e) {
                    // Log.e(TAG, "Could not read from file '" + INTERFACE_FILE
                    // + "'.", e);
                }
            }
        } catch (FileNotFoundException e) {
            // Log.e(TAG, "Could not open file '" + INTERFACE_FILE +
            // "' for reading.", e);
        }

        /*
         * If we were able to read the lines the lines array list will have some
         * entries.
         */
        for (String line : lines) {
            Matcher matcher = LINE_PATTERN.matcher(line);

            if (matcher.matches()) {
                String deviceName = matcher.group(1).trim();
                int bytesReceived = Integer.parseInt(matcher.group(2));
                int bytesSent = Integer.parseInt(matcher.group(10));
                InterfaceStats stats = Updater.sInterfaceStatsMap.get(deviceName);

                if (stats == null) {
                    stats = new InterfaceStats(deviceName);
                    Updater.sInterfaceStatsMap.put(deviceName, stats);
                }

                // Log.v(TAG, "Device=" + deviceName + " bytes received:" +
                // bytesReceived
                // + " bytes sent:" + bytesSent);

                // update our data record
                stats.setBytesReceived(bytesReceived);
                stats.setBytesSent(bytesSent);
            }
        }
    }
}

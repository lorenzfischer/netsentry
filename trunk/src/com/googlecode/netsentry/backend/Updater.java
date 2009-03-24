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
import android.database.Cursor;
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

    /**
     * Whenever some component wants to issue an update of the counters it can
     * broadcast an intent with this name and this receiver will listen for it.
     */
    public static final String ACTION_UPDATE_COUNTERS = "com.googlecode.netsentry.ACTION_UPDATE_COUNTERS";

    /** TAG for logging. */
    private static final String TAG = "ns.Updater";

    /**
     * This is a virtual file of the OS containing counter values for the
     * various network interfaces of the phone. As far as I'm concerned this is
     * not the nicest way to read the information about network traffic, but it
     */
    private static final String INTERFACE_FILE = "/proc/self/net/dev";

    /** Standard projection for all the columns of an interface stats record. */
    private static final String[] PROJECTION = new String[] { InterfaceStatsColumns._ID, // 0
            InterfaceStatsColumns.INTERFACE_NAME, // 1
            InterfaceStatsColumns.BYTES_RECEIVED, // 2
            InterfaceStatsColumns.BYTES_SENT, // 3
            InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM, // 4
            InterfaceStatsColumns.BYTES_SENT_SYSTEM, // 5
            InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, // 6
            InterfaceStatsColumns.INTERFACE_ALIAS, // 7
            InterfaceStatsColumns.NOTIFICATION_LEVEL // 8
    };

    /*
     * TODO maybe we could increase the speed by only grouping on the fields of
     * interest.
     * 
     * TODO try scanner and see if its faster
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
    private static final Map<String, InterfaceStats> sInterfacesStatsMap = new ConcurrentHashMap<String, InterfaceStats>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        ContentResolver resolver = context.getContentResolver();
        final int usageThresholdMedium = Configuration.getUsageThresholdMedium(context);
        final int usageThresholdHigh = Configuration.getUsageThresholdHigh(context);

        Log.d(TAG, "Updating InterfaceStatsProvider..");

        if (Updater.updateInterfaceStats(context)) {
            /*
             * If we have some change, check if we need to send notifications
             */
            Cursor cursor = resolver.query(InterfaceStatsProvider.CONTENT_URI, PROJECTION, null,
                    null, null);

            if ((cursor != null) && (cursor.getCount() > 0)) {
                Notification notification = null;
                int notificationCreated = 0; // 0=no, 1=low, 2=high

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    long bytesLimit = cursor.getLong(6);

                    if (bytesLimit > 0) {
                        long bytesTotal;
                        long usage;
                        int oldNotificationLevel = cursor.getInt(8);
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
                            resolver.update(InterfaceStatsProvider.CONTENT_URI, values,
                                    InterfaceStatsColumns._ID + "=" + cursor.getLong(0), null);
                        }
                    }

                    cursor.moveToNext();
                }

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
                    typeIntent.setType(InterfaceStatsProvider.CONTENT_TYPE);
                    contentIntent = PendingIntent.getActivity(context, 0, typeIntent, 0);

                    if (notificationCreated == 2) {
                        notification.setLatestEventInfo(context, context
                                .getText(R.string.notification_usage_level_high_title), context
                                .getText(R.string.notification_usage_level_high), contentIntent);

                    } else if (notificationCreated == 1) {
                        notification.setLatestEventInfo(context, context
                                .getText(R.string.notification_usage_level_medium_title), context
                                .getText(R.string.notification_usage_level_medium), contentIntent);
                    }

                    notificationManager.cancel(Configuration.NOTIFICATION_ID_USAGE);
                    notificationManager.notify(Configuration.NOTIFICATION_ID_USAGE, notification);
                }
            }
        }
    }

    /**
     * This method issues an update of the interface stats database. This method
     * will not send any notifications to the system.
     * 
     * @param the
     *            context we can use to retrieve resources.
     * @return true if anything was updated at all, false otherwise.
     */
    public static boolean updateInterfaceStats(Context context) {
        boolean updateIssued = false;
        ContentResolver resolver = context.getContentResolver();

        Updater.updateDataMap();

        for (InterfaceStats stats : sInterfacesStatsMap.values()) {
            long bytesReceivedCurrent = stats.getBytesReceived();
            long bytesSentCurrent = stats.getBytesSent();

            /*
             * We only want a record in the database if we have any non-zero
             * values for the given record.
             */
            if (bytesReceivedCurrent > 0 || bytesSentCurrent > 0) {
                Cursor cursor = resolver.query(InterfaceStatsProvider.CONTENT_URI, PROJECTION,
                        InterfaceStatsColumns.INTERFACE_NAME + "='" + stats.getInterfaceName()
                                + "'", null, null);

                if ((cursor == null) || (cursor.getCount() == 0)) {
                    // no entry for the device exists
                    ContentValues values;

                    values = new ContentValues();
                    values.put(InterfaceStatsColumns.INTERFACE_NAME, stats.getInterfaceName());
                    values.put(InterfaceStatsColumns.INTERFACE_ALIAS, getAliasForName(context,
                            stats.getInterfaceName()));
                    values.put(InterfaceStatsColumns.BYTES_RECEIVED, Long
                            .valueOf(bytesReceivedCurrent));
                    values.put(InterfaceStatsColumns.BYTES_SENT, Long.valueOf(bytesSentCurrent));
                    values.put(InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM, Long
                            .valueOf(bytesReceivedCurrent));
                    values.put(InterfaceStatsColumns.BYTES_SENT_SYSTEM, Long
                            .valueOf(bytesSentCurrent));

                    // put default transmission limit and default automatic
                    // reset for 3g devices
                    if (stats.getInterfaceName().startsWith(
                            InterfaceStatsProvider.INTERFACE_NAME_TYPE_3G)) {
                        values.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT,
                                Configuration.DEFAULT_TRANSMISSION_LIMIT);
                        values.put(InterfaceStatsColumns.RESET_CRON_EXPRESSION,
                                Configuration.CRON_EVERY_MONTH);
                    }

                    // store values into the table
                    resolver.insert(InterfaceStatsProvider.CONTENT_URI, values);

                    updateIssued = true;
                } else {
                    /*
                     * entry exists, so update its contents if the delta values
                     * are > 0
                     */
                    long bytesReceivedDelta = 0;
                    long bytesSentDelta = 0;
                    long bytesReceivedDb;
                    long bytesSentDb;
                    // the system values when we checked last
                    long bytesReceivedLast;
                    long bytesSentLast;

                    cursor.moveToFirst();

                    bytesReceivedDb = cursor.getLong(2);
                    bytesSentDb = cursor.getLong(3);
                    bytesReceivedLast = cursor.getLong(4);
                    bytesSentLast = cursor.getLong(5);

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
                        Log.v(TAG, "Values for device " + stats.getInterfaceName()
                                + " changed! bytes received delta: " + bytesReceivedDelta
                                + " bytes sent delta:" + bytesSentDelta);

                        values.put(InterfaceStatsColumns.BYTES_RECEIVED, Long
                                .valueOf(bytesReceivedDb));
                        values.put(InterfaceStatsColumns.BYTES_SENT, Long.valueOf(bytesSentDb));
                        values.put(InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM, Long
                                .valueOf(bytesReceivedCurrent));
                        values.put(InterfaceStatsColumns.BYTES_SENT_SYSTEM, Long
                                .valueOf(bytesSentCurrent));
                        values.put(InterfaceStatsColumns.LAST_UPDATE, System.currentTimeMillis());

                        // store values into the table
                        resolver.update(InterfaceStatsProvider.CONTENT_URI, values,
                                InterfaceStatsColumns._ID + "=" + cursor.getLong(0), null);

                        updateIssued = true;
                    }

                }

                // close cursor if it was opened
                if (cursor != null) {
                    cursor.close();
                }
            }
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
            return context.getString(R.string.provider_interface_alias_3g);
        } else if (interfaceName.startsWith(InterfaceStatsProvider.INTERFACE_NAME_TYPE_ETHERNET)) {
            return context.getString(R.string.provider_interface_alias_ethernet);
        }

        return interfaceName;
    }

    /**
     * Reads the network counters and updates the static information map
     * {@link #sInterfacesStatsMap}. After the execution of this method this map
     * will contain up-to-date information about byte counters of all the
     * networking interfaces available to the android system.
     */
    private static void updateDataMap() {
        FileReader fstream = null;
        BufferedReader in = null;
        ArrayList<String> lines = new ArrayList<String>();

        try {

            fstream = new FileReader("/proc/self/net/dev");

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
                    Log.e(TAG, "Could not read from file '" + INTERFACE_FILE + "'.", e);
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not open file '" + INTERFACE_FILE + "' for reading.", e);
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
                InterfaceStats stats = Updater.sInterfacesStatsMap.get(deviceName);

                if (stats == null) {
                    stats = new InterfaceStats(deviceName);
                    Updater.sInterfacesStatsMap.put(deviceName, stats);
                }

                Log.v(TAG, "Device=" + deviceName + " bytes received:" + bytesReceived
                        + " bytes sent:" + bytesSent);

                // update our data record
                stats.setBytesReceived(bytesReceived);
                stats.setBytesSent(bytesSent);
            }
        }
    }
}

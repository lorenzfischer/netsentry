/**
 * 
 */
package com.googlecode.netsentry.backend;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * This class operates on {@link ResetCountersIntent} intents. It will expect an
 * uri that points to an object of the mime type
 * {@link InterfaceStatsProvider#CONTENT_TYPE_STATS_ITEM} and will throw an
 * {@link IllegalArgumentException} if it is called on anything else.
 * 
 * @author lorenz fischer
 */
public class Resetter extends BroadcastReceiver {

    /**
     * Whenever some component wants to issue a reset of the counters it can
     * broadcast an intent with this name and this receiver will listen for it.
     */
    public static final String ACTION_RESET_COUNTERS = "com.googlecode.netsentry.ACTION_RESET_COUNTERS";

    /** {@inheritDoc} */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.resolveType(context).equals(InterfaceStatsProvider.CONTENT_TYPE_STATS_ITEM)) {
            throw new IllegalArgumentException("This receiver can only work on "
                    + InterfaceStatsProvider.CONTENT_TYPE_STATS_ITEM + " data items.");
        }

        /*
         * Reset the counters of the interface statistics entry that the context
         * menu is opened for.
         */
        Uri interfaceStatsUri = intent.getData();
        ContentValues values = new ContentValues();
        values.put(InterfaceStatsColumns.BYTES_RECEIVED, Long.valueOf(0L));
        values.put(InterfaceStatsColumns.BYTES_SENT, Long.valueOf(0L));
        values.put(InterfaceStatsColumns.LAST_UPDATE, System.currentTimeMillis());
        values.put(InterfaceStatsColumns.LAST_RESET, System.currentTimeMillis());
        values.put(InterfaceStatsColumns.NOTIFICATION_LEVEL, Long.valueOf(0L));
        context.getContentResolver().update(interfaceStatsUri, values, null, null);
    }

    /**
     * This method broadcasts an intent that will be caught by the
     * {@link Resetter} and will reset the counters. By calling this method you
     * can initiate the reset process without having to wait for the reset to
     * happen. This will return immediately after having broadcasted the intent.
     * 
     * @param the
     *            context to use when broadcasting the intent.
     * @param interfaceStatsUri
     *            the record whose counters should be reset.
     */
    public static void broadcastResetIntent(Context context, Uri interfaceStatsUri) {
        context.sendBroadcast(createResetterIntent(interfaceStatsUri));
    }

    /**
     * Creates an intent that can be broadcasted over the context in order to
     * issue a reset of the counters for the record defined by
     * <code>interfaceStatsUri</code>.
     * 
     * @param interfaceStatsUri
     *            the record whose counters should be reset.
     * @return the intent that can be broadcasted in order to issue the reset.
     */
    public static Intent createResetterIntent(Uri interfaceStatsUri) {
        Intent resetIntent = new Intent(Resetter.ACTION_RESET_COUNTERS);
        resetIntent.setData(interfaceStatsUri);
        return resetIntent;
    }
}

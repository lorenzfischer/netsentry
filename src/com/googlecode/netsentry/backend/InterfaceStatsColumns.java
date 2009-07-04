package com.googlecode.netsentry.backend;

import android.provider.BaseColumns;

/**
 * This class contains all the column names needed to store the interface
 * statistics in the SqlLite database.
 * 
 * @author lorenz fischer
 */
public final class InterfaceStatsColumns implements BaseColumns {

    /** This class cannot be instantiated. */
    private InterfaceStatsColumns() {
    }

    /**
     * The default sort order for this table.
     */
    public static final String DEFAULT_SORT_ORDER = "InterfaceName DESC";

    /**
     * The name of the interface these stats are for.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String INTERFACE_NAME = "InterfaceName";
    
    /**
     * The discriminator of an entry enables us to maintain multiple entries for any given
     * interface record in the database. For example for mobile devices (rmnetX) we
     * can choose to save the phone number and/or the carrier name along with
     * the interface name. This in turn gives us the possibility to have separate
     * counter values for different SIM cards plugged into the Android device.
     * 
     * This value is <code>null</code> by default.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String INTERFACE_DISCRIMINATOR = "InterfaceDiscriminator";

    /**
     * The alias to show in the interface list.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String INTERFACE_ALIAS = "InterfaceAlias";

    /**
     * The amount of bytes received through this interface.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String BYTES_RECEIVED = "BytesReceived";

    /**
     * The amount of bytes sent through this interface.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String BYTES_SENT = "BytesSent";

    /**
     * The limit up to which free transmission of data is allowed.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String BYTES_TRANSMISSION_LIMIT = "BytesTransmissionLimit";

    /**
     * The cron expression for when to reset the counters.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String RESET_CRON_EXPRESSION = "ResetCronExpression";

    /**
     * This defines if a record should be shown in the overview list. The user
     * has to deliberately click on the "Show Hidden Interfaces" menu item in
     * order to make non visible entries visible.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String SHOW_IN_LIST = "ShowInList";

    /**
     * This column tells the application which notification level has already
     * been made visible to the user.
     * 
     * <ol start="0"> <li>(Zero) means no notification has been made visible for
     * the interface yet.</li> <li>(One) means a medium usage level reached
     * notification has been made visible for this interface.</li> <li>
     * (Two) means a medium usage level reached notification has been made
     * visible for this interface.</li> </ol>
     * 
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String NOTIFICATION_LEVEL = "NotificationLevel";

    /**
     * The timestamp for when the counters (!) were last updated.
     * <P>
     * Type: INTEGER (long from System.curentTimeMillis())
     * </P>
     */
    public static final String LAST_UPDATE = "LastUpdate";

    /**
     * The timestamp for when the counters (!) were last reset.
     * <P>
     * Type: INTEGER (long from System.curentTimeMillis())
     * </P>
     */
    public static final String LAST_RESET = "LastReset";

}

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
	 * The value currently stored as the system's value for bytes received. The
	 * "system's" counters are somewhat volatile values, since they might get
	 * reset when an interface is turned off. Too, we cannot rely on having the
	 * current counter value of the system in a static member field of the
	 * updater, since this would increase our counter values whenever that class
	 * gets reloaded (for example when the software is newly installed on the
	 * device).
	 * 
	 * <P>
	 * Type: INTEGER
	 * </P>
	 */
	public static final String BYTES_RECEIVED_SYSTEM = "BytesReceivedSystem";

	/**
	 * The amount of bytes sent through this interface.
	 * <P>
	 * Type: INTEGER
	 * </P>
	 */
	public static final String BYTES_SENT = "BytesSent";

	/**
	 * The value currently stored as the system's value for bytes sent. The
	 * "system's" counters are somewhat volatile values, since they might get
	 * reset when an interface is turned off. Too, we cannot rely on having the
	 * current counter value of the system in a static member field of the
	 * updater, since this would increase our counter values whenever that class
	 * gets reloaded (for example when the software is newly installed on the
	 * device).
	 * 
	 * <P>
	 * Type: INTEGER
	 * </P>
	 */
	public static final String BYTES_SENT_SYSTEM = "BytesSentSystem";

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
	 * <ol start="0"> <li>(Zero) means no notification has been made
	 * visible for the interface yet.</li> <li>(One) means a medium usage level
	 * reached notification has been made visible for this interface.</li> <li>
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

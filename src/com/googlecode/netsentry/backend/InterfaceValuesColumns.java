package com.googlecode.netsentry.backend;

import android.provider.BaseColumns;

/**
 * This class contains all the column names needed to store the interface value
 * records in the SqLite database. The value records are used to keep track of
 * what the system counter were set to every time an update is issued. This
 * makes the application more fault tolerant against crashes. An alternative
 * solution would have been to keep the current system counters in static
 * variables, but these would be lost when the application crashes, which might
 * then have caused false values being inserted into the stats table after the
 * crash.
 * 
 * @author lorenz fischer
 */
public class InterfaceValuesColumns implements BaseColumns {

    /** This class cannot be instantiated. */
    private InterfaceValuesColumns() {
    }

    /**
     * The default sort order for this table.
     */
    public static final String DEFAULT_SORT_ORDER = "InterfaceName DESC";

    /**
     * The name of the interface these values are kept for. The records in this
     * table don't need a discriminator, since they're always globally valid.
     * The linux system itself only keeps one counter value, and not multiple
     * ones.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String INTERFACE_NAME = "InterfaceName";

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
     * The timestamp for when the values were last updated. This value could be empty (right
     * after the database update. 
     *  
     * <P>
     * Type: INTEGER (long from System.curentTimeMillis())
     * </P>
     */
    public static final String LAST_UPDATE = "LastUpdate";

}

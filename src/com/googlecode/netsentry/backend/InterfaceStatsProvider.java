/**
 * 
 */
package com.googlecode.netsentry.backend;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.googlecode.netsentry.R;

/**
 * <p>
 * This content provider can be used to retrieve information about the bytes
 * sent and received by the network interfaces of an android phone.
 * </p>
 * 
 * <p>
 * Adapted from the com.example.android.notepad.NotePadProvider example.
 * </p>
 * 
 * @author lorenz fischer
 */
public class InterfaceStatsProvider extends ContentProvider {

    /** The tag information for the logging facility. */
    private static final String TAG = "ns.InterfaceStatsProvider";

    /** The authority is used so we can put together the content provider uri. */
    public static final String AUTHORITY = "com.googlecode.netsentry";

    /**
     * The content:// style URL for this table.
     */
    public static final Uri CONTENT_URI_STATS = Uri.parse("content://" + AUTHORITY
            + "/interfacestats");

    /**
     * The MIME type of {@link #CONTENT_URI_STATS} providing a directory of
     * interface statisics entries.
     */
    public static final String CONTENT_TYPE_STATS = "vnd.android.cursor.dir/interfacestats";

    /**
     * The MIME type of a {@link #CONTENT_URI_STATS} sub-directory of a single
     * interface statistics entry.
     */
    public static final String CONTENT_TYPE_STATS_ITEM = "vnd.android.cursor.item/interfacestats";

    /**
     * The content:// style URL for this table.
     */
    public static final Uri CONTENT_URI_VALUES = Uri.parse("content://" + AUTHORITY
            + "/interfacevalues");

    /**
     * The MIME type of {@link #CONTENT_URI_VALUES} providing a directory of
     * interface statisics entries.
     */
    public static final String CONTENT_TYPE_VALUES = "vnd.android.cursor.dir/interfacevalues";

    /**
     * The MIME type of a {@link #CONTENT_URI_VALUES} sub-directory of a single
     * interface statistics entry.
     */
    public static final String CONTENT_TYPE_VALUES_ITEM = "vnd.android.cursor.item/interfacevalues";

    // these values are used to map interface names to interface types
    /** The matching group for wifi type interface names. */
    public final static String INTERFACE_NAME_TYPE_WIFI = "tiwlan";

    /** The matching group for 3g type interface names. */
    public final static String INTERFACE_NAME_TYPE_3G = "rmnet";

    /** The matching group for ethernet type interface names. */
    public final static String INTERFACE_NAME_TYPE_ETHERNET = "eth";

    /** The name of the database file. */
    private static final String DATABASE_NAME = "netsentry.db";

    /**
     * The version of the database.
     * <ol>
     * <li>Initial Version [1]</li>
     * <li>New Discriminator Column [2]</li>
     * </ol>
     */
    private static final int DATABASE_VERSION = 2;

    /** The name of the table holding the interface statistics records. */
    private static final String INTERFACE_STATS_TABLE_NAME = "InterfaceStats";

    /**
     * The name of the table holding the counter values as they were retrieved
     * last.
     */
    private static final String INTERFACE_VALUES_TABLE_NAME = "InterfaceValues";

    /**
     * The uri matcher helps to parse uri strings sent by clients. TODO is
     * UriMatcher thread safe? Need to know for {@link #getType(Uri)} needs to
     * be thread safe..
     */
    private static final UriMatcher sUriMatcher;

    /* These IDs are needed by the URI matcher. */
    private static final int INTERFACESTATS = 1;
    private static final int INTERFACESTATS_ID = 2;
    private static final int INTERFACEVALUES = 3;
    private static final int INTERFACEVALUES_ID = 4;

    /** The projection map for the stats table. */
    private static final HashMap<String, String> sProjectionMapStats;

    /** The projection map for the values table. */
    private static final HashMap<String, String> sProjectionMapValues;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "interfacestats", INTERFACESTATS);
        sUriMatcher.addURI(AUTHORITY, "interfacestats/#", INTERFACESTATS_ID);
        sUriMatcher.addURI(AUTHORITY, "interfacevalues", INTERFACEVALUES);
        sUriMatcher.addURI(AUTHORITY, "interfacevalues/#", INTERFACEVALUES_ID);

        sProjectionMapStats = new HashMap<String, String>();
        sProjectionMapStats.put(InterfaceStatsColumns._ID, InterfaceStatsColumns._ID);
        sProjectionMapStats.put(InterfaceStatsColumns.INTERFACE_NAME,
                InterfaceStatsColumns.INTERFACE_NAME);
        sProjectionMapStats.put(InterfaceStatsColumns.INTERFACE_DISCRIMINATOR,
                InterfaceStatsColumns.INTERFACE_DISCRIMINATOR);
        sProjectionMapStats.put(InterfaceStatsColumns.INTERFACE_ALIAS,
                InterfaceStatsColumns.INTERFACE_ALIAS);
        sProjectionMapStats.put(InterfaceStatsColumns.BYTES_RECEIVED,
                InterfaceStatsColumns.BYTES_RECEIVED);

        sProjectionMapStats.put(InterfaceStatsColumns.BYTES_SENT, InterfaceStatsColumns.BYTES_SENT);

        sProjectionMapStats.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT,
                InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT);
        sProjectionMapStats.put(InterfaceStatsColumns.RESET_CRON_EXPRESSION,
                InterfaceStatsColumns.RESET_CRON_EXPRESSION);
        sProjectionMapStats.put(InterfaceStatsColumns.SHOW_IN_LIST,
                InterfaceStatsColumns.SHOW_IN_LIST);
        sProjectionMapStats.put(InterfaceStatsColumns.NOTIFICATION_LEVEL,
                InterfaceStatsColumns.NOTIFICATION_LEVEL);
        sProjectionMapStats.put(InterfaceStatsColumns.LAST_UPDATE,
                InterfaceStatsColumns.LAST_UPDATE);
        sProjectionMapStats.put(InterfaceStatsColumns.LAST_RESET, InterfaceStatsColumns.LAST_RESET);

        sProjectionMapValues = new HashMap<String, String>();
        sProjectionMapValues.put(InterfaceValuesColumns._ID, InterfaceValuesColumns._ID);
        sProjectionMapValues.put(InterfaceValuesColumns.BYTES_SENT_SYSTEM,
                InterfaceValuesColumns.BYTES_SENT_SYSTEM);
        sProjectionMapValues.put(InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM,
                InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM);
        sProjectionMapValues.put(InterfaceValuesColumns.LAST_UPDATE,
                InterfaceValuesColumns.LAST_UPDATE);
    }

    /** Helper for the creation and updates of the database table. */
    private DatabaseHelper databaseHelper;

    /** {@inheritDoc} */
    @Override
    public boolean onCreate() {
        this.databaseHelper = new DatabaseHelper(getContext());
        return true; // successfully loaded
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy = null;

        switch (InterfaceStatsProvider.sUriMatcher.match(uri)) {
        case INTERFACESTATS:
            qb.setTables(INTERFACE_STATS_TABLE_NAME);
            qb.setProjectionMap(InterfaceStatsProvider.sProjectionMapStats);
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = InterfaceStatsColumns.DEFAULT_SORT_ORDER;
            }
            break;
        case INTERFACESTATS_ID:
            qb.setTables(INTERFACE_STATS_TABLE_NAME);
            qb.setProjectionMap(InterfaceStatsProvider.sProjectionMapStats);
            qb.appendWhere(InterfaceStatsColumns._ID + "=" + uri.getPathSegments().get(1));
            break;
        case INTERFACEVALUES:
            qb.setTables(INTERFACE_VALUES_TABLE_NAME);
            qb.setProjectionMap(InterfaceStatsProvider.sProjectionMapValues);
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = InterfaceValuesColumns.DEFAULT_SORT_ORDER;
            }
            break;
        case INTERFACEVALUES_ID:
            qb.setTables(INTERFACE_VALUES_TABLE_NAME);
            qb.setProjectionMap(InterfaceStatsProvider.sProjectionMapValues);
            qb.appendWhere(InterfaceValuesColumns._ID + "=" + uri.getPathSegments().get(1));
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (orderBy == null) {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = this.databaseHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        SQLiteDatabase db;
        long rowId = -1;
        Uri insertedUri = null;

        values = new ContentValues(initialValues);
        db = this.databaseHelper.getWritableDatabase();

        // Validate the requested uri
        switch (InterfaceStatsProvider.sUriMatcher.match(uri)) {
        case INTERFACESTATS:
            // fill in default values (if not set already)
            setDefaultInterfaceStats(values);

            // Make sure that the fields are all set
            if (!values.containsKey(InterfaceStatsColumns.INTERFACE_NAME)) {
                throw new IllegalArgumentException("Could not store entry: missing interface name.");
            }

            if (!values.containsKey(InterfaceStatsColumns.INTERFACE_ALIAS)) {
                // use interface name as default interface alias
                values.put(InterfaceStatsColumns.INTERFACE_ALIAS, values
                        .getAsString(InterfaceStatsColumns.INTERFACE_NAME));
            }

            rowId = db.insert(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME,
                    InterfaceStatsColumns.RESET_CRON_EXPRESSION, values);
            if (rowId > 0) {
                insertedUri = ContentUris.withAppendedId(InterfaceStatsProvider.CONTENT_URI_STATS,
                        rowId);
            }
            break;

        case INTERFACEVALUES:
            // fill in default values (if not set already)
            setDefaultInterfaceValues(values);
            // Make sure that the fields are all set
            if (!values.containsKey(InterfaceValuesColumns.INTERFACE_NAME)) {
                throw new IllegalArgumentException("Could not store entry: missing interface name.");
            }

            // that null column hack will never be used, since we checked on the
            // name of the interface
            // already.
            rowId = db.insert(InterfaceStatsProvider.INTERFACE_VALUES_TABLE_NAME,
                    InterfaceValuesColumns.INTERFACE_NAME, values);
            if (rowId > 0) {
                insertedUri = ContentUris.withAppendedId(InterfaceStatsProvider.CONTENT_URI_VALUES,
                        rowId);
            }
            break;

        default:
            throw new IllegalArgumentException("Cannot Insert URI " + uri);
        }

        if (insertedUri != null) {
            getContext().getContentResolver().notifyChange(insertedUri, null);
            return insertedUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    /**
     * This method fills in all default values needed for a single record of
     * type {@link #INTERFACESTATS}.
     */
    private static void setDefaultInterfaceStats(ContentValues values) {
        if (!values.containsKey(InterfaceStatsColumns.BYTES_RECEIVED)) {
            values.put(InterfaceStatsColumns.BYTES_RECEIVED, Long.valueOf(0));
        }
        if (!values.containsKey(InterfaceStatsColumns.BYTES_SENT)) {
            values.put(InterfaceStatsColumns.BYTES_SENT, Long.valueOf(0));
        }
        if (!values.containsKey(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT)) {
            // the default transmission limit is 0 = unlimited
            values.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, Long.valueOf(0));
        }
        if (!values.containsKey(InterfaceStatsColumns.NOTIFICATION_LEVEL)) {
            values.put(InterfaceStatsColumns.NOTIFICATION_LEVEL, Long.valueOf(0));
        }
        if (!values.containsKey(InterfaceStatsColumns.SHOW_IN_LIST)) {
            // not hidden is default
            values.put(InterfaceStatsColumns.SHOW_IN_LIST, Long.valueOf(1));
        }
        if (!values.containsKey(InterfaceStatsColumns.LAST_UPDATE)) {
            values.put(InterfaceStatsColumns.LAST_UPDATE, Long.valueOf(System.currentTimeMillis()));
        }
        if (!values.containsKey(InterfaceStatsColumns.LAST_RESET)) {
            values.put(InterfaceStatsColumns.LAST_RESET, Long.valueOf(System.currentTimeMillis()));
        }
    }

    /**
     * This method fills in all default values needed for a single record of
     * type {@link #INTERFACEVALUES}.
     */
    private static void setDefaultInterfaceValues(ContentValues values) {
        if (!values.containsKey(InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM)) {
            values.put(InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM, Long.valueOf(0));
        }
        if (!values.containsKey(InterfaceValuesColumns.BYTES_SENT_SYSTEM)) {
            values.put(InterfaceValuesColumns.BYTES_SENT_SYSTEM, Long.valueOf(0));
        }
        if (!values.containsKey(InterfaceValuesColumns.LAST_UPDATE)) {
            values
                    .put(InterfaceValuesColumns.LAST_UPDATE, Long.valueOf(System
                            .currentTimeMillis()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = this.databaseHelper.getWritableDatabase();
        int count;
        switch (InterfaceStatsProvider.sUriMatcher.match(uri)) {
        case InterfaceStatsProvider.INTERFACESTATS:
            count = db.delete(INTERFACE_STATS_TABLE_NAME, where, whereArgs);
            break;
        case InterfaceStatsProvider.INTERFACESTATS_ID:
            String statsId = uri.getPathSegments().get(1);
            count = db.delete(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME,
                    InterfaceStatsColumns._ID + "=" + statsId
                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        case InterfaceStatsProvider.INTERFACEVALUES:
            count = db.delete(INTERFACE_VALUES_TABLE_NAME, where, whereArgs);
            break;
        case InterfaceStatsProvider.INTERFACEVALUES_ID:
            String valuesId = uri.getPathSegments().get(1);
            count = db.delete(InterfaceStatsProvider.INTERFACE_VALUES_TABLE_NAME,
                    InterfaceValuesColumns._ID + "=" + valuesId
                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = this.databaseHelper.getWritableDatabase();
        int count;

        switch (InterfaceStatsProvider.sUriMatcher.match(uri)) {
        case InterfaceStatsProvider.INTERFACESTATS:
            count = db.update(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME, values, where,
                    whereArgs);
            break;
        case InterfaceStatsProvider.INTERFACESTATS_ID:
            String statsId = uri.getPathSegments().get(1);
            count = db.update(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME, values,
                    InterfaceStatsColumns._ID + "=" + statsId
                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        case InterfaceStatsProvider.INTERFACEVALUES:
            count = db.update(InterfaceStatsProvider.INTERFACE_VALUES_TABLE_NAME, values, where,
                    whereArgs);
            break;
        case InterfaceStatsProvider.INTERFACEVALUES_ID:
            String valuesId = uri.getPathSegments().get(1);
            count = db.update(InterfaceStatsProvider.INTERFACE_VALUES_TABLE_NAME, values,
                    InterfaceValuesColumns._ID + "=" + valuesId
                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        String result;

        switch (sUriMatcher.match(uri)) {
        case INTERFACESTATS:
            result = InterfaceStatsProvider.CONTENT_TYPE_STATS;
            break;

        case INTERFACESTATS_ID:
            result = InterfaceStatsProvider.CONTENT_TYPE_STATS_ITEM;
            break;

        case INTERFACEVALUES:
            result = InterfaceStatsProvider.CONTENT_TYPE_VALUES;
            break;

        case INTERFACEVALUES_ID:
            result = InterfaceStatsProvider.CONTENT_TYPE_VALUES_ITEM;
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return result;
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        private final Context mContext;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            ContentValues valuesLoopback = new ContentValues();

            Log.v(TAG, "Creating database for interface statistics.");

            // TODO what happens when the database creation fails
            db.execSQL("CREATE TABLE " + INTERFACE_STATS_TABLE_NAME + " ("
                    + InterfaceStatsColumns._ID + " INTEGER PRIMARY KEY,"
                    + InterfaceStatsColumns.INTERFACE_NAME + " TEXT,"
                    + InterfaceStatsColumns.INTERFACE_DISCRIMINATOR + " TEXT,"
                    + InterfaceStatsColumns.INTERFACE_ALIAS + " TEXT,"
                    + InterfaceStatsColumns.BYTES_RECEIVED + " INTEGER,"
                    + InterfaceStatsColumns.BYTES_SENT + " INTEGER,"
                    + InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT + " INTEGER,"
                    + InterfaceStatsColumns.RESET_CRON_EXPRESSION + " TEXT,"
                    + InterfaceStatsColumns.SHOW_IN_LIST + " INTEGER,"
                    + InterfaceStatsColumns.NOTIFICATION_LEVEL + " INTEGER,"
                    + InterfaceStatsColumns.LAST_UPDATE + " INTEGER,"
                    + InterfaceStatsColumns.LAST_RESET + " INTEGER" + ");");

            db.execSQL("CREATE TABLE " + INTERFACE_VALUES_TABLE_NAME + " ("
                    + InterfaceValuesColumns._ID + " INTEGER PRIMARY KEY,"
                    + InterfaceValuesColumns.INTERFACE_NAME + " TEXT,"
                    + InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM + " INTEGER,"
                    + InterfaceValuesColumns.BYTES_SENT_SYSTEM + " INTEGER,"
                    + InterfaceValuesColumns.LAST_UPDATE + " INTEGER" + ");");

            // create the loop back device which will be hidden by default
            valuesLoopback.put(InterfaceStatsColumns.INTERFACE_NAME, "lo");
            valuesLoopback.put(InterfaceStatsColumns.INTERFACE_ALIAS, mContext
                    .getString(R.string.provider_interface_alias_loopback));
            valuesLoopback.put(InterfaceStatsColumns.SHOW_IN_LIST, Long.valueOf(0));
            valuesLoopback.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, Long.valueOf(0));
            InterfaceStatsProvider.setDefaultInterfaceStats(valuesLoopback);
            db.insert(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME,
                    InterfaceStatsColumns.RESET_CRON_EXPRESSION, valuesLoopback);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            try {
                if (oldVersion < newVersion) {
                    Log.d(TAG, "Upgrading database from version " + oldVersion + " to "
                            + newVersion);

                    // upgrades to version 2
                    if (oldVersion < 2) {

                        try {
                            // add discriminator column
                            db.execSQL("ALTER TABLE "
                                    + InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME
                                    + " ADD COLUMN "
                                    + InterfaceStatsColumns.INTERFACE_DISCRIMINATOR + " TEXT;");

                            // create values table
                            db.execSQL("CREATE TABLE " + INTERFACE_VALUES_TABLE_NAME + " ("
                                    + InterfaceValuesColumns._ID + " INTEGER PRIMARY KEY,"
                                    + InterfaceValuesColumns.INTERFACE_NAME + " TEXT,"
                                    + InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM + " INTEGER,"
                                    + InterfaceValuesColumns.BYTES_SENT_SYSTEM + " INTEGER,"
                                    + InterfaceValuesColumns.LAST_UPDATE + " INTEGER" + ");");

                            // copy BytesSentSystem and BytesReceivedSystem
                            // values
                            db.execSQL("INSERT INTO "  + INTERFACE_VALUES_TABLE_NAME +
                                    "(" + InterfaceValuesColumns.INTERFACE_NAME + ","
                                    + InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM + ","
                                    + InterfaceValuesColumns.BYTES_SENT_SYSTEM + ") SELECT " + 
                                    InterfaceValuesColumns.INTERFACE_NAME + ","
                                    + InterfaceValuesColumns.BYTES_RECEIVED_SYSTEM + ","
                                    + InterfaceValuesColumns.BYTES_SENT_SYSTEM + 
                                    " FROM " + INTERFACE_STATS_TABLE_NAME + ";");

                            // we do not drop the old columns for now. they
                            // don't
                            // interfere
                        } catch (SQLException e) {
                            throw new DatabaseUpgradeException("Error while trying to update"
                                    + " the database to comply with version 2.", e);
                        }
                    }

                } else if (oldVersion > newVersion) {
                    throw new DatabaseUpgradeException("Downgrading database from version "
                            + oldVersion + " to " + newVersion
                            + " which will erase all existing data");
                }
            } catch (DatabaseUpgradeException e) {
                Log.d(TAG, e.getMessage());
                db.execSQL("DROP TABLE IF EXISTS "
                        + InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS "
                        + InterfaceStatsProvider.INTERFACE_VALUES_TABLE_NAME);
                onCreate(db);
            }

        }
    }

    private static class DatabaseUpgradeException extends RuntimeException {

        /**
         * @param detailMessage
         *            the message giving an explanation for what went wrong.
         * @param throwable
         *            the throwable that was the cause for the exception.
         */
        public DatabaseUpgradeException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        /**
         * @param detailMessage
         *            the message giving an explanation for what went wrong.
         */
        public DatabaseUpgradeException(String detailMessage) {
            super(detailMessage);
        }

    }

}

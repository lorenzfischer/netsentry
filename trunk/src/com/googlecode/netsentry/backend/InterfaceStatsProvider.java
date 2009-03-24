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
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/interfacestats");

	/**
	 * The MIME type of {@link #CONTENT_URI} providing a directory of interface
	 * statisics entries.
	 */
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/interfacestats";

	/**
	 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
	 * interface statistics entry.
	 */
	public static final String ITEM_CONTENT_TYPE = "vnd.android.cursor.item/interfacestats";

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
	 * The version of the database. 1. Initial Version
	 */
	private static final int DATABASE_VERSION = 1;

	/** The name of the table holding the interface statistics records. */
	private static final String INTERFACE_STATS_TABLE_NAME = "InterfaceStats";

	/**
	 * The uri matcher helps to parse uri strings sent by clients. TODO is
	 * UriMatcher thread safe? Need to know for {@link #getType(Uri)} needs to
	 * be thread safe..
	 */
	private static final UriMatcher uriMatcher;

	private static final int INTERFACESTATS = 1;
	private static final int INTERFACESTATS_ID = 2;

	/** The projection map. */
	private static final HashMap<String, String> sProjectionMap;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(InterfaceStatsProvider.AUTHORITY, "interfacestats", INTERFACESTATS);
		uriMatcher.addURI(InterfaceStatsProvider.AUTHORITY, "interfacestats/#", INTERFACESTATS_ID);

		sProjectionMap = new HashMap<String, String>();
		sProjectionMap.put(InterfaceStatsColumns._ID, InterfaceStatsColumns._ID);
		sProjectionMap.put(InterfaceStatsColumns.INTERFACE_NAME,
				InterfaceStatsColumns.INTERFACE_NAME);
		sProjectionMap.put(InterfaceStatsColumns.INTERFACE_ALIAS,
				InterfaceStatsColumns.INTERFACE_ALIAS);
		sProjectionMap.put(InterfaceStatsColumns.BYTES_RECEIVED,
				InterfaceStatsColumns.BYTES_RECEIVED);
		sProjectionMap.put(InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM,
				InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM);
		sProjectionMap.put(InterfaceStatsColumns.BYTES_SENT, InterfaceStatsColumns.BYTES_SENT);
		sProjectionMap.put(InterfaceStatsColumns.BYTES_SENT_SYSTEM,
				InterfaceStatsColumns.BYTES_SENT_SYSTEM);
		sProjectionMap.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT,
				InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT);
		sProjectionMap.put(InterfaceStatsColumns.RESET_CRON_EXPRESSION,
				InterfaceStatsColumns.RESET_CRON_EXPRESSION);
		sProjectionMap.put(InterfaceStatsColumns.SHOW_IN_LIST, InterfaceStatsColumns.SHOW_IN_LIST);
		sProjectionMap.put(InterfaceStatsColumns.NOTIFICATION_LEVEL,
				InterfaceStatsColumns.NOTIFICATION_LEVEL);
		sProjectionMap.put(InterfaceStatsColumns.LAST_UPDATE, InterfaceStatsColumns.LAST_UPDATE);
		sProjectionMap.put(InterfaceStatsColumns.LAST_RESET, InterfaceStatsColumns.LAST_RESET);
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

		switch (InterfaceStatsProvider.uriMatcher.match(uri)) {
		case INTERFACESTATS:
			qb.setTables(INTERFACE_STATS_TABLE_NAME);
			qb.setProjectionMap(InterfaceStatsProvider.sProjectionMap);
			break;

		case INTERFACESTATS_ID:
			qb.setTables(INTERFACE_STATS_TABLE_NAME);
			qb.setProjectionMap(InterfaceStatsProvider.sProjectionMap);
			qb.appendWhere(InterfaceStatsColumns._ID + "=" + uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = InterfaceStatsColumns.DEFAULT_SORT_ORDER;
		} else {
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
		long rowId;

		// Validate the requested uri
		if (InterfaceStatsProvider.uriMatcher.match(uri) != INTERFACESTATS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		values = new ContentValues(initialValues);

		// fill in default values (if not set already)
		setDefaultInterfaceStatsValues(values);

		// Make sure that the fields are all set
		if (!values.containsKey(InterfaceStatsColumns.INTERFACE_NAME)) {
			throw new IllegalArgumentException("Could not store entry: missing interface name.");
		}

		if (!values.containsKey(InterfaceStatsColumns.INTERFACE_ALIAS)) {
			// use interface name as default interface alias
			values.put(InterfaceStatsColumns.INTERFACE_ALIAS, values
					.getAsString(InterfaceStatsColumns.INTERFACE_NAME));
		}

		db = this.databaseHelper.getWritableDatabase();
		rowId = db.insert(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME,
				InterfaceStatsColumns.RESET_CRON_EXPRESSION, values);
		if (rowId > 0) {
			Uri interfaceStatsUri = ContentUris.withAppendedId(InterfaceStatsProvider.CONTENT_URI,
					rowId);
			getContext().getContentResolver().notifyChange(interfaceStatsUri, null);
			return interfaceStatsUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * This method fills in all default values needed for a single record of
	 * type {@link #INTERFACESTATS}.
	 */
	private static void setDefaultInterfaceStatsValues(ContentValues values) {
		if (!values.containsKey(InterfaceStatsColumns.BYTES_RECEIVED)) {
			values.put(InterfaceStatsColumns.BYTES_RECEIVED, Long.valueOf(0));
		}
		if (!values.containsKey(InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM)) {
			values.put(InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM, Long.valueOf(0));
		}
		if (!values.containsKey(InterfaceStatsColumns.BYTES_SENT)) {
			values.put(InterfaceStatsColumns.BYTES_SENT, Long.valueOf(0));
		}
		if (!values.containsKey(InterfaceStatsColumns.BYTES_SENT_SYSTEM)) {
			values.put(InterfaceStatsColumns.BYTES_SENT_SYSTEM, Long.valueOf(0));
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

	/** {@inheritDoc} */
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = this.databaseHelper.getWritableDatabase();
		int count;
		switch (InterfaceStatsProvider.uriMatcher.match(uri)) {
		case InterfaceStatsProvider.INTERFACESTATS:
			count = db.delete(INTERFACE_STATS_TABLE_NAME, where, whereArgs);
			break;

		case InterfaceStatsProvider.INTERFACESTATS_ID:
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME,
					InterfaceStatsColumns._ID + "=" + noteId
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
		switch (InterfaceStatsProvider.uriMatcher.match(uri)) {
		case InterfaceStatsProvider.INTERFACESTATS:
			count = db.update(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME, values, where,
					whereArgs);
			break;

		case InterfaceStatsProvider.INTERFACESTATS_ID:
			String noteId = uri.getPathSegments().get(1);
			count = db.update(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME, values,
					InterfaceStatsColumns._ID + "=" + noteId
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

		switch (uriMatcher.match(uri)) {
		case INTERFACESTATS:
			result = InterfaceStatsProvider.CONTENT_TYPE;
			break;

		case INTERFACESTATS_ID:
			result = InterfaceStatsProvider.ITEM_CONTENT_TYPE;
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
			// TODO what happens when the database creation failes
			db.execSQL("CREATE TABLE " + INTERFACE_STATS_TABLE_NAME + " ("
					+ InterfaceStatsColumns._ID + " INTEGER PRIMARY KEY,"
					+ InterfaceStatsColumns.INTERFACE_NAME + " TEXT,"
					+ InterfaceStatsColumns.INTERFACE_ALIAS + " TEXT,"
					+ InterfaceStatsColumns.BYTES_RECEIVED + " INTEGER,"
					+ InterfaceStatsColumns.BYTES_RECEIVED_SYSTEM + " INTEGER,"
					+ InterfaceStatsColumns.BYTES_SENT + " INTEGER,"
					+ InterfaceStatsColumns.BYTES_SENT_SYSTEM + " INTEGER,"
					+ InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT + " INTEGER,"
					+ InterfaceStatsColumns.RESET_CRON_EXPRESSION + " TEXT,"
					+ InterfaceStatsColumns.SHOW_IN_LIST + " INTEGER,"
					+ InterfaceStatsColumns.NOTIFICATION_LEVEL + " INTEGER,"
					+ InterfaceStatsColumns.LAST_UPDATE + " INTEGER,"
					+ InterfaceStatsColumns.LAST_RESET + " INTEGER" + ");");

			// create the loop back device which will be hidden by default
			valuesLoopback.put(InterfaceStatsColumns.INTERFACE_NAME, "lo");
			valuesLoopback.put(InterfaceStatsColumns.INTERFACE_ALIAS, mContext
					.getString(R.string.provider_interface_alias_loopback));
			valuesLoopback.put(InterfaceStatsColumns.SHOW_IN_LIST, Long.valueOf(0));
			valuesLoopback.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, Long.valueOf(0));
			InterfaceStatsProvider.setDefaultInterfaceStatsValues(valuesLoopback);
			db.insert(InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME,
					InterfaceStatsColumns.RESET_CRON_EXPRESSION, valuesLoopback);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + InterfaceStatsProvider.INTERFACE_STATS_TABLE_NAME);
			onCreate(db);
		}
	}

}

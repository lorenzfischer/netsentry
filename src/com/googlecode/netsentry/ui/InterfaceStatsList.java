package com.googlecode.netsentry.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.backend.Bootstrapper;
import com.googlecode.netsentry.backend.Configuration;
import com.googlecode.netsentry.backend.InterfaceStatsColumns;
import com.googlecode.netsentry.backend.InterfaceStatsProvider;
import com.googlecode.netsentry.backend.Resetter;
import com.googlecode.netsentry.backend.Updater;
import com.googlecode.netsentry.util.StringUtilities;

/**
 * This activity lists all the records of the {@link InterfaceStatsProvider}.
 * 
 * @author lorenz fischer
 */
public class InterfaceStatsList extends ListActivity {

    // /** TAG for logging. */
    // private static final String TAG = "ns.InterfaceList";

    /**
     * Standard projection for the interesting columns of a normal interface
     * stats record.
     */
    private static final String[] PROJECTION = new String[] { InterfaceStatsColumns._ID, // 0
            InterfaceStatsColumns.INTERFACE_NAME, // 1
            InterfaceStatsColumns.INTERFACE_ALIAS, // 2
            InterfaceStatsColumns.BYTES_RECEIVED, // 3
            InterfaceStatsColumns.BYTES_SENT, // 4
            InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, // 5
            InterfaceStatsColumns.LAST_UPDATE, // 6
            InterfaceStatsColumns.LAST_RESET // 7
    };

    /** The id for the "reset counters" context menu item. */
    private static final int CONTEXT_MENU_ITEM_RESET_COUNTERS = Menu.FIRST;

    /** The id for the "edit" context menu item. */
    private static final int CONTEXT_MENU_ITEM_EDIT = Menu.FIRST + 1;

    /** The id for the "about" option menu item. */
    private static final int OPTION_MENU_ITEM_PREFERENCES = Menu.FIRST;

    /** The id for the "about" option menu item. */
    private static final int OPTION_MENU_ITEM_ABOUT = Menu.FIRST + 1;

    /** Constant for the set transmission limit dialog. */
    private static final int DIALOG_ABOUT = 1;

    /**
     * The cursor through which the database will be accessed, this cursor will
     * be closed in the {@link #onDestroy()} method.
     */
    private Cursor mCursor;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager notificationManager;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(InterfaceStatsProvider.CONTENT_URI);
        }

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);

        /*
         * Perform a managed query. The Activity will handle closing and
         * requerying the cursor when needed.
         */
        mCursor = managedQuery(getIntent().getData(), PROJECTION,
                InterfaceStatsColumns.SHOW_IN_LIST + " = 1", null,
                InterfaceStatsColumns.DEFAULT_SORT_ORDER);

        // Used to map the rows in the cursor to list views.
        ResourceCursorAdapter adapter = new ResourceCursorAdapter(this,
                R.layout.interfacestats_list_item, mCursor) {

            private DateFormat mFormat = new SimpleDateFormat();

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                final int usageLevelMedium = Configuration
                        .getUsageThresholdMedium(InterfaceStatsList.this);
                final int usageLevelHigh = Configuration
                        .getUsageThresholdHigh(InterfaceStatsList.this);
                ImageView typeIcon = (ImageView) view
                        .findViewById(R.id.interfaceList_Item_image_interfaceType);
                TextView alias = (TextView) view
                        .findViewById(R.id.interfaceList_Item_interfaceAlias);
                TextView counter = (TextView) view.findViewById(R.id.interfaceList_Item_counter);
                TextView lastResetValue = (TextView) view
                        .findViewById(R.id.interfaceList_Item_lastReset_value);
                long bytesTotal;
                long bytesLimit;

                bytesTotal = cursor.getLong(3) + cursor.getLong(4);
                bytesLimit = cursor.getLong(5);

                typeIcon.setImageResource(InterfaceIcon.getResourceIdForInterface(cursor
                        .getString(1)));
                alias.setText(cursor.getString(2));

                lastResetValue.setText(mFormat.format(new Date(cursor.getLong(7))));

                if (bytesLimit > 0) {
                    long usage;

                    usage = Double.valueOf((1.0D * bytesTotal / bytesLimit) * 100).longValue();

                    // set color according to usage
                    if (usage >= usageLevelHigh) {
                        counter.setTextColor(getResources().getColor(R.color.solid_red));
                    } else if (usage >= usageLevelMedium) {
                        counter.setTextColor(getResources().getColor(R.color.solid_yellow));
                    } else {
                        counter.setTextColor(getResources().getColor(R.color.solid_white));
                    }

                    counter.setText(StringUtilities.formatDataNumber(bytesTotal) + " / "
                            + StringUtilities.formatDataNumber(bytesLimit) + " (" + usage + "%)");
                } else {

                    counter.setTextColor(getResources().getColor(R.color.solid_white));
                    counter.setText(StringUtilities.formatDataNumber(bytesTotal));
                }

            }
        };

        setListAdapter(adapter);

        /*
         * Initialization will only happen once after booting or starting
         * netsentry for the first time
         */
        Bootstrapper.initializeSystem(this);

        // clear any notifications that might be up
        notificationManager.cancel(Configuration.NOTIFICATION_ID_USAGE);
    }

    /** {@inheritDoc} */
    @Override
    protected void onResume() {
        super.onResume();

        // Issue an update
        sendBroadcast(new Intent(Updater.ACTION_UPDATE_COUNTERS));
    }

    /** {@inheritDoc} */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // close cursor
        mCursor.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // Log.e(TAG, "Bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {

            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(2));

        menu.add(0, InterfaceStatsList.CONTEXT_MENU_ITEM_EDIT, 0, R.string.menu_item_edit);
        menu.add(0, InterfaceStatsList.CONTEXT_MENU_ITEM_RESET_COUNTERS, 1,
                R.string.menu_item_reset_counters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            // Log.e(TAG, "Bad menuInfo", e);
            return false;
        }

        // get the Uri for the interfaceStats item this menu is called upon
        Uri interfaceStatsUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        switch (item.getItemId()) {

        case InterfaceStatsList.CONTEXT_MENU_ITEM_RESET_COUNTERS: {
            Resetter.broadcastResetIntent(getApplicationContext(), interfaceStatsUri);
            return true;
        }

        case InterfaceStatsList.CONTEXT_MENU_ITEM_EDIT: {

            /* Display the edit view for the selected record. */
            startEditorActivity(interfaceStatsUri);
            return true;
        }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        startEditorActivity(ContentUris.withAppendedId(getIntent().getData(), id));
    }

    /**
     * Starts the editor activity for a single interface stats record in the
     * list.
     * 
     * @param interfaceStatsUri
     *            the item to show the editor activity for.
     */
    private void startEditorActivity(Uri interfaceStatsUri) {
        startActivity(new Intent(Intent.ACTION_EDIT, interfaceStatsUri));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, OPTION_MENU_ITEM_PREFERENCES, 0, R.string.menu_item_preferences).setIcon(
                android.R.drawable.ic_menu_preferences);

        menu.add(0, OPTION_MENU_ITEM_ABOUT, 1, R.string.menu_item_about).setIcon(
                android.R.drawable.ic_menu_help);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case OPTION_MENU_ITEM_PREFERENCES:
            startActivity(new Intent(ApplicationPreferences.ACTION_EDIT_PREFERENCES));
            return true;

        case OPTION_MENU_ITEM_ABOUT:
            showDialog(DIALOG_ABOUT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog result = null;

        switch (id) {

        case DIALOG_ABOUT:
            result = new AboutDialog(InterfaceStatsList.this);
            break;
        }
        return result;
    }
}

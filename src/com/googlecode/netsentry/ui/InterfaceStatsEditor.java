package com.googlecode.netsentry.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.backend.Configuration;
import com.googlecode.netsentry.backend.InterfaceStatsColumns;
import com.googlecode.netsentry.backend.Resetter;
import com.googlecode.netsentry.backend.scheduler.CronScheduler;
import com.googlecode.netsentry.util.StringUtilities;
import com.googlecode.netsentry.widget.DataPicker;
import com.googlecode.netsentry.widget.DataPickerDialog;

/**
 * This activity lets the user edit and view one single interface statistics
 * record stored in the database.
 * 
 * @author lorenz fischer
 */
public class InterfaceStatsEditor extends Activity {

    /** Constant for the set transmission limit dialog. */
    private static final int DIALOG_SET_TRANSMISSION_LIMIT = 1;

    /** Constant for the reset counters confirmation dialog. */
    private static final int DIALOG_RESET_COUNTERS = 2;

    /** Standard projection for all the columns of an interface stats record. */
    private static final String[] PROJECTION = new String[] { InterfaceStatsColumns._ID, // 0
            InterfaceStatsColumns.INTERFACE_NAME, // 1
            InterfaceStatsColumns.INTERFACE_ALIAS, // 2
            InterfaceStatsColumns.BYTES_RECEIVED, // 3
            InterfaceStatsColumns.BYTES_SENT, // 4
            InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, // 5
            InterfaceStatsColumns.RESET_CRON_EXPRESSION, // 6
            InterfaceStatsColumns.SHOW_IN_LIST, // 7
            InterfaceStatsColumns.LAST_UPDATE // 8
    };

    /**
     * This array will hold the selection possibilities of the "automatic reset"
     * spinner.
     * 
     * TODO find out how we can load these values once only.
     */
    private CronExpressionEntry[] mCronExpressions;

    // references to view components
    private TextView mInterfaceName;
    private TextView mInterfaceAlias;
    private TextView mDataReceived;
    private TextView mDataSent;
    private TextView mDataTotal;
    private TextView mTransmissionLimit;
    private ImageView mInterfaceTypeIcon;
    private Button mSetTransmissionLimitButton;
    private Button mResetCountersButton;
    private Spinner mAutoReset;

    /** This observer will update the gui as necessary. */
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateGui();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout for this activity
        setContentView(R.layout.interfacestats_editor);

        mInterfaceName = (TextView) findViewById(R.id.editor_interface_name);
        mInterfaceAlias = (TextView) findViewById(R.id.editor_interface_alias);
        mDataReceived = (TextView) findViewById(R.id.editor_data_received);
        mDataSent = (TextView) findViewById(R.id.editor_data_sent);
        mDataTotal = (TextView) findViewById(R.id.editor_data_total);
        mTransmissionLimit = (TextView) findViewById(R.id.editor_transmission_limit);
        mInterfaceTypeIcon = (ImageView) findViewById(R.id.editor_image_interface_type);
        mSetTransmissionLimitButton = (Button) findViewById(R.id.editor_button_set_transmission_limit);
        mResetCountersButton = (Button) findViewById(R.id.editor_button_reset_counters);
        mAutoReset = (Spinner) findViewById(R.id.editor_spinner_auto_reset);

        // load values for spinner from resource bundle
        mCronExpressions = new CronExpressionEntry[] {
                new CronExpressionEntry(getString(R.string.editor_cron_not_scheduled), null),
                new CronExpressionEntry(getString(R.string.editor_cron_every_day),
                        Configuration.CRON_EVERY_DAY),
                new CronExpressionEntry(getString(R.string.editor_cron_every_week),
                                Configuration.CRON_EVERY_WEEK),
                new CronExpressionEntry(getString(R.string.editor_cron_every_month),
                        Configuration.CRON_EVERY_MONTH) };

        // fill in values for the spinner
        ArrayAdapter<CronExpressionEntry> adapter = new ArrayAdapter<CronExpressionEntry>(this,
                android.R.layout.simple_spinner_item, mCronExpressions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAutoReset.setAdapter(adapter);

        // make sure we get notified about changes to our record
        getContentResolver()
                .registerContentObserver(getIntent().getData(), false, mContentObserver);
        updateGui(); // update the gui the first time

        // attach listeners
        mSetTransmissionLimitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_SET_TRANSMISSION_LIMIT);
            }
        });

        mResetCountersButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_RESET_COUNTERS);
            }
        });

        mAutoReset.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Intent resetterIntent;
                String selectedCronExpression;
                ContentValues values;

                selectedCronExpression = mCronExpressions[position].getCronExpression();
                values = new ContentValues();
                values.put(InterfaceStatsColumns.RESET_CRON_EXPRESSION, selectedCronExpression);
                // store values into the table
                getContentResolver().update(getIntent().getData(), values, null, null);

                resetterIntent = Resetter.createResetterIntent(getIntent().getData());
                if (selectedCronExpression != null) {
                    // schedule job (this stops already scheduled jobs)
                    CronScheduler.scheduleJob(getApplicationContext(), resetterIntent,
                            selectedCronExpression);
                } else {
                    // stop any running timer and start a new one
                    CronScheduler.stopScheduledJob(InterfaceStatsEditor.this, resetterIntent);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // not possible
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // stop listening to content updates
        getContentResolver().unregisterContentObserver(mContentObserver);
    }

    /**
     * This method reads out the data from the data provider and puts it into
     * the gui components.
     */
    private void updateGui() {
        long bytesReceived, bytesSent, bytesTotal, bytesLimit;
        Cursor entry = getContentResolver().query(getIntent().getData(), PROJECTION, null, null,
                null);

        // get entry and move cursor to the first and only row
        entry.moveToFirst();

        bytesReceived = entry.getLong(3);
        bytesSent = entry.getLong(4);
        bytesTotal = bytesReceived + bytesSent;
        bytesLimit = entry.getLong(5);

        mInterfaceName.setText(entry.getString(1));
        mInterfaceAlias.setText(entry.getString(2));
        mDataReceived.setText(StringUtilities.formatDataNumber(bytesReceived));
        mDataSent.setText(StringUtilities.formatDataNumber(bytesSent));
        mDataTotal.setText(StringUtilities.formatDataNumber(bytesTotal));

        if (bytesLimit > 0) {
            mTransmissionLimit.setText(StringUtilities.formatDataNumber(bytesLimit));
        } else {
            mTransmissionLimit.setText(R.string.infinity_sign);
        }

        mInterfaceTypeIcon.setImageResource(InterfaceIcon.getResourceIdForInterface(entry
                .getString(1)));

        /*
         * This is not very efficient for big n, but we assume we only have n<10
         * records here, so this should be good enough.
         */
        int cronExpressionCount = mCronExpressions.length;
        for (int i = 0; i < cronExpressionCount; i++) {
            if ((mCronExpressions[i].getCronExpression() == null && entry.getString(6) == null)
                    || (mCronExpressions[i].getCronExpression() != null && mCronExpressions[i]
                            .getCronExpression().equals(entry.getString(6)))) {
                mAutoReset.setSelection(i);
                break;
            }
        }

        // close cursor
        entry.close();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog result = null;

        switch (id) {
        case DIALOG_SET_TRANSMISSION_LIMIT:
            Cursor entry = getContentResolver().query(getIntent().getData(), PROJECTION, null,
                    null, null);

            // get entry and move cursor to the first and only row
            entry.moveToFirst();

            result = new DataPickerDialog(InterfaceStatsEditor.this, entry.getLong(5),
                    new DataPicker.OnBytesChangedListener() {
                        @Override
                        public void onChanged(DataPicker view, long oldValue, long newValue) {
                            ContentValues values = new ContentValues();
                            values.put(InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, Long
                                    .valueOf(newValue));
                            values
                                    .put(InterfaceStatsColumns.NOTIFICATION_LEVEL, Integer
                                            .valueOf(0));
                            // store values into the table
                            getContentResolver().update(getIntent().getData(), values, null, null);
                        }
                    }, getString(R.string.editor_transmission_limit_info_text));
            result.setTitle(R.string.dialog_transmission_limit_title);
            
            // close cursor
            entry.close();
            
            break;

        case DIALOG_RESET_COUNTERS:
            result = new AlertDialog.Builder(this)
                    .setMessage(R.string.editor_dialog_reset_counters).setPositiveButton(
                            android.R.string.ok, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Resetter.broadcastResetIntent(getApplicationContext(),
                                            getIntent().getData());
                                }
                            }).setNegativeButton(android.R.string.cancel, null).create();
            break;
        }
        return result;
    }

    /**
     * This class will be used by the spinner, so the user can select from
     * predefined cron expressions.
     * 
     * It is designed immutable, so we can share instances of it between
     * threads.
     * 
     * This class is thread safe.
     * 
     * @author lorenz fischer
     */
    private static class CronExpressionEntry {
        /**
         * The cron expression. <code>null</code> means not scheduled for reset.
         */
        private final String mCronExpression;

        /**
         * This will be printed inside the spinner through the
         * {@link #toString()} method.
         */
        private final String mLabel;

        /**
         * Creates an entry that can be rendered inside the spinner using the
         * {@link ArrayAdapter} class.
         * 
         * @param label
         *            the label to render inside the spinner.
         * @param cronExpression
         *            the cron expression the label stands for.
         */
        public CronExpressionEntry(String label, String cronExpression) {
            mLabel = label;
            mCronExpression = cronExpression;
        }

        /**
         * You can call this method on an entry in order to get the proper cron
         * expression that the user has selected.
         * 
         * @return the cron expression this entry stands for.
         */
        public String getCronExpression() {
            return mCronExpression;
        }

        /**
         * This method will be called by the {@link ArrayAdapter} in order to
         * render it inside the spinner.
         */
        @Override
        public String toString() {
            return mLabel;
        }
    }

}

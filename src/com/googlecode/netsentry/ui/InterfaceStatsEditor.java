package com.googlecode.netsentry.ui;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

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
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.backend.InterfaceStatsColumns;
import com.googlecode.netsentry.backend.Resetter;
import com.googlecode.netsentry.backend.Updater;
import com.googlecode.netsentry.backend.scheduler.CronScheduler;
import com.googlecode.netsentry.util.CronExpression;
import com.googlecode.netsentry.util.Misc;
import com.googlecode.netsentry.widget.DataPicker;
import com.googlecode.netsentry.widget.DataPickerDialog;
import com.googlecode.netsentry.widget.cronpicker.CronPicker;
import com.googlecode.netsentry.widget.listener.OnValueChangedListener;

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
	private static final String[] PROJECTION = new String[] {
			InterfaceStatsColumns._ID, // 0
			InterfaceStatsColumns.INTERFACE_NAME, // 1
			InterfaceStatsColumns.INTERFACE_ALIAS, // 2
			InterfaceStatsColumns.BYTES_RECEIVED, // 3
			InterfaceStatsColumns.BYTES_SENT, // 4
			InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT, // 5
			InterfaceStatsColumns.RESET_CRON_EXPRESSION, // 6
			InterfaceStatsColumns.SHOW_IN_LIST, // 7
			InterfaceStatsColumns.LAST_UPDATE // 8
	};

	/** This formatter is used to render the next scheduled reset date. */
	private static DateFormat sDateFormat;

	/** This formatter is used to render the next scheduled reset time. */
	private static DateFormat sTimeFormat;

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
	private TextView mAutoResetNext;
	private CronPicker mAutoResetCronPicker;

	/** This observer will update the gui as necessary. */
	private ContentObserver mContentObserver = new ContentObserver(
			new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			updateGui();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sDateFormat = android.text.format.DateFormat.getDateFormat(this);
		sTimeFormat = android.text.format.DateFormat.getTimeFormat(this);

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
		mAutoResetNext = (TextView) findViewById(R.id.editor_auto_reset_next);
		mAutoResetCronPicker = (CronPicker) findViewById(R.id.editor_auto_reset_cron_picker);

		// initial update of ui components
		updateGui();

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

		mAutoResetCronPicker
				.setOnValueChangedListener(new OnValueChangedListener<String>() {
					@Override
					public void onChanged(String oldValue, String newValue) {
						Intent resetterIntent;
						ContentValues values;

						values = new ContentValues();
						values.put(InterfaceStatsColumns.RESET_CRON_EXPRESSION,
								newValue);
						// store values into the table
						getContentResolver().update(getIntent().getData(),
								values, null, null);

						resetterIntent = Resetter
								.createResetterIntent(getIntent().getData());
						if (newValue != null) {
							// schedule job (this stops already scheduled jobs)
							CronScheduler.scheduleJob(getApplicationContext(),
									resetterIntent, newValue);
						} else {
							// stop any running timer
							CronScheduler.stopScheduledJob(
									InterfaceStatsEditor.this, resetterIntent);
						}

					}
				});
	}

	@Override
	protected void onResume() {
		super.onResume();

		/* Make sure we get notified about changes to our record */
		getContentResolver().registerContentObserver(getIntent().getData(),
				false, mContentObserver);
		/*
		 * Issue an update (which in turn will be caught by the content
		 * observer)
		 */
		sendBroadcast(new Intent(Updater.ACTION_UPDATE_COUNTERS));
	}

	@Override
	protected void onPause() {
		super.onPause();

		// stop listening to content updates
		getContentResolver().unregisterContentObserver(mContentObserver);
	}

	/**
	 * This method reads out the data from the data provider and puts it into
	 * the gui components.
	 */
	private void updateGui() {
		String cronExpression;
		CronExpression nextResetExpression = null;
		Date nextResetDate = null;
		long bytesReceived, bytesSent, bytesTotal, bytesLimit;
		Cursor entry = getContentResolver().query(getIntent().getData(),
				PROJECTION, null, null, null);

		// get entry and move cursor to the first and only row
		entry.moveToFirst();

		bytesReceived = entry.getLong(3);
		bytesSent = entry.getLong(4);
		bytesTotal = bytesReceived + bytesSent;
		bytesLimit = entry.getLong(5);
		cronExpression = entry.getString(6);

		mInterfaceName.setText(entry.getString(1));
		mInterfaceAlias.setText(entry.getString(2));
		mDataReceived.setText(Formatter.formatFileSize(this, bytesReceived));
		mDataSent.setText(Formatter.formatFileSize(this, bytesSent));
		mDataTotal.setText(Formatter.formatFileSize(this, bytesTotal));

		if (bytesLimit > 0) {
			mTransmissionLimit.setText(Formatter.formatFileSize(this,
					bytesLimit));
		} else {
			mTransmissionLimit.setText(R.string.infinity_sign);
		}

		mInterfaceTypeIcon.setImageResource(InterfaceIcon
				.getResourceIdForInterface(entry.getString(1)));

		if (cronExpression != null) {
			try {
				nextResetExpression = new CronExpression(cronExpression);
				nextResetDate = nextResetExpression.getNextValidTimeAfter(new Date());
				mAutoResetNext.setText(getString(R.string.editor_next_auto_reset_scheduled,
						sDateFormat.format(nextResetDate), sTimeFormat.format(nextResetDate)));
			} catch (ParseException e) {
				// this should not be possible, because we are the only ones
				// setting the
				// reset expression.
			}
		} else {
			mAutoResetNext
					.setText(R.string.editor_next_auto_reset_not_scheduled);
		}

		if (!Misc.areEqual(mAutoResetCronPicker.getCronExpression(),
				cronExpression)) {
			mAutoResetCronPicker.setCronExpression(cronExpression);
		}

		// close cursor
		entry.close();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog result = null;

		switch (id) {
		case DIALOG_SET_TRANSMISSION_LIMIT:
			Cursor entry = getContentResolver().query(getIntent().getData(),
					PROJECTION, null, null, null);

			// get entry and move cursor to the first and only row
			entry.moveToFirst();

			result = new DataPickerDialog(InterfaceStatsEditor.this,
					entry.getLong(5), new DataPicker.OnBytesChangedListener() {
						@Override
						public void onChanged(DataPicker view, long oldValue,
								long newValue) {
							ContentValues values = new ContentValues();
							values.put(
									InterfaceStatsColumns.BYTES_TRANSMISSION_LIMIT,
									Long.valueOf(newValue));
							values.put(
									InterfaceStatsColumns.NOTIFICATION_LEVEL,
									Integer.valueOf(0));
							// store values into the table
							getContentResolver().update(getIntent().getData(),
									values, null, null);
						}
					}, getString(R.string.editor_transmission_limit_info_text));
			result.setTitle(R.string.editor_transmission_limit_dialog_title);

			// close cursor
			entry.close();

			break;

		case DIALOG_RESET_COUNTERS:
			result = new AlertDialog.Builder(this)
					.setMessage(R.string.editor_dialog_reset_counters)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Resetter.broadcastResetIntent(
											getApplicationContext(),
											getIntent().getData());
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.create();
			break;

		}
		return result;
	}

}

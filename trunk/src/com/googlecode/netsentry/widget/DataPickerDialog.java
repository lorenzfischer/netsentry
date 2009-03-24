package com.googlecode.netsentry.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.widget.DataPicker.OnBytesChangedListener;

/**
 * A simple dialog containing a {@link DataPicker}.
 */
public class DataPickerDialog extends AlertDialog implements OnClickListener {

	private final DataPicker mDataPicker;

	/**
	 * @param context
	 *            The context the dialog is to run in.
	 * @param bytes
	 *            the amount of bytes to preselect.
	 * @param callBack
	 *            How the parent is notified that the data amount is set.
	 * @param infoText
	 *            if provided, this text will be shown underneath the data
	 *            picket widget. If null, no text will be shown.
	 */
	public DataPickerDialog(Context context, long bytes, OnBytesChangedListener callBack,
			String infoText) {
		super(context);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.data_picker_dialog, null);

		setButton(context.getText(R.string.button_close), this);
		setView(view);
		mDataPicker = (DataPicker) view.findViewById(R.id.data_picker_dialog_picker);
		mDataPicker.init(bytes, callBack);

		if (infoText != null) {
			TextView infoTextView = (TextView) view.findViewById(R.id.data_picker_dialog_info_text);
			infoTextView.setText(R.string.editor_transmission_limit_info_text);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// store the current value of the date picker
		mDataPicker.clearFocus();
	}

}
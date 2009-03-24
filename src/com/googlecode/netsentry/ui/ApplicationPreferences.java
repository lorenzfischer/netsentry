/**
 * 
 */
package com.googlecode.netsentry.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.googlecode.netsentry.R;

/**
 * Using this activity the user can edit the preferences of the NetSentry
 * application.
 * 
 * @author lorenz fischer
 */
public class ApplicationPreferences extends PreferenceActivity {

	/** This activity can be started by an intent with this action. */
	public final static String ACTION_EDIT_PREFERENCES = "com.googlecode.netsentry.ACTION_EDIT_PREFERENCES";

	/** {@inheritDoc} */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the XML preferences file
		addPreferencesFromResource(R.xml.preferences);
	}

}

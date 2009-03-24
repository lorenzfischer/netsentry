/**
 * 
 */
package com.googlecode.netsentry.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.googlecode.netsentry.R;

/**
 * A simple dialog containing the about dialog layout.
 * 
 * @author lorenz fischer
 */
public class AboutDialog extends AlertDialog {

    // /** TAG for logging. */
    // private static final String TAG = "ns.AboutDialog";

    /**
     * @param context
     *            The context the dialog is to run in.
     */
    public AboutDialog(Context context) {
        super(context);
        TextView disclaimer;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.about_dialog, null);

        setButton(context.getText(R.string.button_close), (OnClickListener) null);
        setIcon(R.drawable.icon);
        setTitle(R.string.about_dialog_title);
        setView(view);

        // setting about text with application version number
        disclaimer = (TextView) view.findViewById(R.id.about_dialog_disclaimer);
        disclaimer.setText(context.getString(R.string.about_dialog_disclaimer,
                getApplicationVersion(context)));
    }

    /**
     * @param context
     *            the context to use for the version retrieval.
     * @return the version of the NetSentry application or an empty string if
     *         the version could not be retrieved.
     */
    private Object getApplicationVersion(Context context) {
        String version = "";
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Log.e(TAG, "Could not retrieve application version.", e);
        }
        return version;
    }

}

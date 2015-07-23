/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.cronpicker;

import java.text.DateFormatSymbols;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.util.Misc;
import com.googlecode.netsentry.widget.listener.OnValueChangedListener;

/**
 * This cron details instance is used when the user selects a weekly interval.
 * It lets the user choose which day of the week should be used for the weekly
 * interval.
 * 
 * @author lorenz fischer
 */
public class WeeklyCronDetails extends LinearLayout implements CronDetails {

    /** The pattern used to find the day in a given cron expression. */
    public static final String CRON_EXPRESSION_PATTERN = "0 0 0 \\? \\* ([0-9]) \\*";

    /**
     * This pattern will be used to match cron expressions agains, in order to
     * find out which day of the week the pattern stands for.
     */
    private static final Pattern CRON_EXPRESSION_COMPILED_PATTERN = Pattern
            .compile(CRON_EXPRESSION_PATTERN);

    /**
     * The pattern used for creating a valid cron expression using the selected
     * day of week.
     */
    public static final String CRON_EXPRESSION_FORMAT = "0 0 0 ? * %1$s *";

    /**
     * This instance will be called whenever the currently set cron expression
     * changes because the user issued the change over the ui
     */
    private OnValueChangedListener<String> mOnValueChangedListener;

    /**
     * The cron expression currently being represented by this details instance.
     */
    private String mCronExpression = String.format(CRON_EXPRESSION_FORMAT, 1);

    /** The day of the week will be selectable using this spinner. */
    private Spinner mDayOfWeek;

    /**
     * Constructor.
     * 
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public WeeklyCronDetails(Context context) {
        this(context, null);
    }

    /**
     * Constructor.
     * 
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public WeeklyCronDetails(final Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        String[] days = new String[7];
        String[] daySymbols = new DateFormatSymbols().getWeekdays();
        ArrayAdapter<String> adapter;

        inflater.inflate(R.layout.cron_picker_weekly, this, true);

        for (int i = 1; i <= 7; i++) {
            days[i - 1] = daySymbols[i];
        }

        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, days);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mDayOfWeek = (Spinner) findViewById(R.id.cron_picker_weekly_days);
        mDayOfWeek.setAdapter(adapter);
        mDayOfWeek.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String oldCronExpression = mCronExpression;
                mCronExpression = String.format(CRON_EXPRESSION_FORMAT, position + 1);

                if (!Misc.areEqual(mCronExpression, oldCronExpression)
                        && mOnValueChangedListener != null) {
                    mOnValueChangedListener.onChanged(oldCronExpression, mCronExpression);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // not possible
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public String getCronExpression() {
        return mCronExpression;
    }

    /** {@inheritDoc} */
    @Override
    public View getView() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void setCronExpression(String cronExpression) {
        Matcher matcher = CRON_EXPRESSION_COMPILED_PATTERN.matcher(cronExpression);
        if (matcher.matches()) {
            int selectedDay = Integer.parseInt(matcher.group(1));
            // prevent callbacks for this update
            mCronExpression = String.format(CRON_EXPRESSION_FORMAT, selectedDay);
            mDayOfWeek.setSelection(selectedDay - 1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOnValueChangedListener(OnValueChangedListener<String> callBack) {
        mOnValueChangedListener = callBack;
    }

}

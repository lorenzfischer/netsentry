/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.cronpicker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.util.Misc;
import com.googlecode.netsentry.widget.NumberPicker;
import com.googlecode.netsentry.widget.listener.OnValueChangedListener;

/**
 * This cron details instance is used when the user selects a monthly interval.
 * It lets the user choose which day of the month should be used for the monthly
 * interval.
 * 
 * @author lorenz fischer
 */
public class MonthlyCronDetails extends RelativeLayout implements CronDetails {

    /** The pattern used to find the day in a given cron expression. */
    public static final String CRON_EXPRESSION_PATTERN = "0 0 0 ([0-9]{1,2}) \\* \\? \\*";

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
    public static final String CRON_EXPRESSION_FORMAT = "0 0 0 %1$s * ? *";

    /**
     * This instance will be called whenever the currently set cron expression
     * changes because the user issued the change over the ui.
     */
    private OnValueChangedListener<String> mOnValueChangedListener;

    /**
     * The cron expression currently being represented by this details instance.
     */
    private String mCronExpression = String.format(CRON_EXPRESSION_FORMAT, 1);

    /** The day of the week will be selectable using this spinner. */
    private NumberPicker mDayOfMonth;

    /**
     * Constructor.
     * 
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public MonthlyCronDetails(Context context) {
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
    public MonthlyCronDetails(final Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.cron_picker_monthly, this, true);
        mDayOfMonth = (NumberPicker) findViewById(R.id.cron_picker_monthly_day_of_month);

        mDayOfMonth.setSpeed(100);
        mDayOfMonth.setRange(1, 31);
        mDayOfMonth.setOnChangeListener(new NumberPicker.OnChangedListener() {

            @Override
            public void onChanged(NumberPicker picker, int oldVal, int newVal) {
                String oldCronExpression = mCronExpression;
                mCronExpression = String.format(CRON_EXPRESSION_FORMAT, newVal);

                if (!Misc.areEqual(mCronExpression, oldCronExpression)
                        && mOnValueChangedListener != null) {
                    mOnValueChangedListener.onChanged(oldCronExpression, mCronExpression);
                }
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
            mDayOfMonth.setCurrent(selectedDay);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOnValueChangedListener(OnValueChangedListener<String> callBack) {
        mOnValueChangedListener = callBack;
    }

}

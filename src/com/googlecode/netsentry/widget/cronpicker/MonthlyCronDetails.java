/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.cronpicker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.googlecode.netsentry.R;
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

    /** This text view will show the currently selected day. */
    private TextView mOnDayText;

    /** Pressing this button lets the user choose the day of the month. */
    private Button mDayOfMonthButton;

    /** This variable holds the current value for the "day of the month". */
    private int mDayOfMonth = 1;

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
        mOnDayText = (TextView) findViewById(R.id.cron_picker_monthly_on_day);
        mOnDayText.setText(getContext().getString(R.string.cron_picker_monthly_on_day,
                Integer.toString(mDayOfMonth)));
        mDayOfMonthButton = (Button) findViewById(R.id.cron_picker_monthly_choose_day_button);
        mDayOfMonthButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DayOfMonthChooserDialog dialog;

                dialog = new DayOfMonthChooserDialog(getContext(), mDayOfMonth,
                        new NumberPicker.OnChangedListener() {

                            @Override
                            public void onChanged(NumberPicker picker, int oldVal, int newVal) {
                                if (newVal != mDayOfMonth) {
                                    String oldCronExpression = getCronExpression();

                                    mDayOfMonth = newVal;
                                    mOnDayText.setText(getContext().getString(
                                            R.string.cron_picker_monthly_on_day,
                                            Integer.toString(mDayOfMonth)));
                                    mOnValueChangedListener.onChanged(oldCronExpression,
                                            getCronExpression());
                                }
                            }
                        });
                dialog.setTitle(R.string.cron_picker_monthly_choose_day);
                dialog.show();
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public String getCronExpression() {
        return String.format(CRON_EXPRESSION_FORMAT, mDayOfMonth);
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
            mDayOfMonth = selectedDay;
            mOnDayText.setText(getContext().getString(R.string.cron_picker_monthly_on_day,
                    Integer.toString(mDayOfMonth)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOnValueChangedListener(OnValueChangedListener<String> callBack) {
        mOnValueChangedListener = callBack;
    }
    
    /**
     * This dialog lets the user choose the day of the month (a number between 1
     * and 31).
     */
    public static class DayOfMonthChooserDialog extends AlertDialog implements
            DialogInterface.OnClickListener {

        /** The day numberpicker instance. */
        private NumberPicker mDayOfMonth;

        /**
         * @param context
         *            The context the dialog is to run in.
         * @param day
         *            the day to preselect.
         * @param callBack
         *            How the parent is notified that the data amount is set.
         */
        public DayOfMonthChooserDialog(Context context, int day,
                NumberPicker.OnChangedListener callback) {
            super(context);

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.cron_picker_monthly_day_dialog, null);

            setButton(context.getText(R.string.button_close), this);
            setView(view);
            mDayOfMonth = (NumberPicker) view.findViewById(R.id.cron_picker_monthly_day_of_month);
            mDayOfMonth.setSpeed(100);
            mDayOfMonth.setRange(1, 31);
            mDayOfMonth.setCurrent(day);
            mDayOfMonth.setOnChangeListener(callback);
        }

        /** {@inheritDoc} */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // store the current value of the date picker
            mDayOfMonth.clearFocus();
        }

        /**
         * @return the day of the month currently being represented by the
         *         number picker.
         */
        public int getDayOfMonth() {
            return mDayOfMonth.getCurrent();
        }

        /**
         * @param day
         *            the day of month to set.
         */
        public void setDayOfMonth(int day) {
            mDayOfMonth.setCurrent(day);
        }

    }

}

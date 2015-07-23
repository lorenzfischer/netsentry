/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.cronpicker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
 * This dialog lets the user choose a regular expression from a predefined list.
 * 
 * @author lorenz fischer
 */
public class CronPicker extends LinearLayout implements OnClickListener {

    /** The resource ids for the textual representation of the interval spinner. */
    private static final int[] sIntervalTextIds = new int[] {//
    R.string.cron_picker_not_scheduled, //
            R.string.cron_picker_daily, //
            R.string.cron_picker_weekly, //
            R.string.cron_picker_monthly };

    /**
     * Regular expression patterns, which are used to preselect the appropriate
     * entry from the spinner, given the currently set cron expression.
     */
    private static final String[] sIntervalPatterns = new String[] { //
    null, // null stands for "not scheduled"
            DailyCronDetails.CRON_EXPRESSION_PATTERN, // daily
            WeeklyCronDetails.CRON_EXPRESSION_PATTERN, // day of week / weekly
            MonthlyCronDetails.CRON_EXPRESSION_PATTERN // day in month / montly
    };

    /** A Spinner with the basic intervals to choose from. */
    private Spinner mBasic;

    /**
     * Using this variable we remember the state we expect the basic spinner to
     * be in and if we receive an update through the OnItemSelectedListener we
     * only propagate this event if we really have a new selection.
     */
    private int mBasicPosition;

    /** This layout will be used to add/remove the detail components. */
    private LinearLayout mDetailsContainer;

    /**
     * This is the component that lets the user further specify his cron
     * expression using more detailed information.
     */
    private CronDetails mDetails = new NullCronDetails();

    /**
     * This instance will be called whenever the currently set cron expression
     * changes because the user issued the change over the ui
     */
    private OnValueChangedListener<String> mOnValueChangedListener;

    /**
     * Constructor.
     * 
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public CronPicker(Context context) {
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
    public CronPicker(final Context context, AttributeSet attrs) {
        super(context, attrs);
        ArrayAdapter<String> adapter;
        String[] intervalValues = new String[4];
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.cron_picker, this, true);
        mBasic = (Spinner) findViewById(R.id.cron_picker_basic);
        mDetailsContainer = (LinearLayout) findViewById(R.id.cron_picker_detail_container);

        for (int i = 0; i < 4; i++) {
            intervalValues[i] = getContext().getString(sIntervalTextIds[i]);
        }

        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
                intervalValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBasic.setAdapter(adapter);
        mBasic.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String oldCronExpression;
                String newCronExpression;

                oldCronExpression = mDetails.getCronExpression();
                setBasic(position);
                newCronExpression = mDetails.getCronExpression();

                if (!Misc.areEqual(newCronExpression, oldCronExpression)
                        && mOnValueChangedListener != null) {
                    mOnValueChangedListener.onChanged(oldCronExpression, newCronExpression);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // not possible
            }
        });

    }

    /**
     * Sets all the components in this picker to reflect the given
     * <code>cronExpression</code>. Calling this method will <b>not</b> invoke
     * any callbacks.
     * 
     * @param cronExpression
     *            the cron expression to preselect.
     */
    public void setCronExpression(String cronExpression) {
        if (!Misc.areEqual(cronExpression, mDetails.getCronExpression())) {
            int newPosition = 0; // new position of basic spinner

            /* The interval spinner */
            if (cronExpression != null) {
                for (int i = 1; i < 4; i++) {
                    if (cronExpression != null && cronExpression.matches(sIntervalPatterns[i])) {
                        newPosition = i;
                        break;
                    }
                }
            }

            setBasic(newPosition);
            mBasic.setSelection(newPosition);
            mDetails.setCronExpression(cronExpression);
        }
    }

    /**
     * @param position
     *            the new position of the basi TODO!!
     */
    private void setBasic(int position) {
        if (mBasicPosition != position) {
            View oldView = mDetails.getView();

            mBasicPosition = position;
            mDetails.setOnValueChangedListener(null);
            switch (position) {
            case 0: // not scheduled
                mDetails = new NullCronDetails();
                break;
            case 1: // daily
                mDetails = new DailyCronDetails();
                break;
            case 2: // day of week
                mDetails = new WeeklyCronDetails(getContext());
                break;
            case 3:
                mDetails = new MonthlyCronDetails(getContext());
                break;
            default:
                break;
            }
            mDetails.setOnValueChangedListener(mOnValueChangedListener);

            /* Apply changes if there were any */
            if (oldView != null) {
                mDetailsContainer.removeView(oldView);
            }
            if (mDetails.getView() != null) {
                mDetailsContainer.addView(mDetails.getView());
            }
        }
    }

    /**
     * Register a listener which will be informed about changes to the currently
     * set cron expression. Call this method with a <code>null</code> value in
     * order to de-register a previously registered listener.
     * 
     * @param callBack
     *            the listener instance.
     */
    public void setOnValueChangedListener(OnValueChangedListener<String> callBack) {
        mOnValueChangedListener = callBack;
        mDetails.setOnValueChangedListener(callBack);
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        mBasic.clearFocus();
    }

    /**
     * @return the cron expression currently being represented by picker.
     */
    public String getCronExpression() {
        return mDetails.getCronExpression();
    }

    // /**
    // * This adapter will be used by the auto-reset dialog, so the user can
    // * choose between weekly and monthly automatic resets.
    // *
    // * @author lorenz fischer
    // */
    // private class CronExpressionAdapter extends BaseExpandableListAdapter {
    //
    // /** The group array contains only two entries. */
    // private final String[] mGroups = {
    // getContext().getString(R.string.editor_cron_monthly),
    // getContext().getString(R.string.editor_cron_weekly) };
    //
    // /**
    // * The first dimension is the groups, the second dimension is the
    // * children of the groups.
    // */
    // private CronExpressionEntry[][] mChildren;
    //
    // /**
    // * Constructs a new instance of {@link CronExpressionAdapter}.
    // */
    // public CronExpressionAdapter() {
    // DateFormat dayFormatter = new SimpleDateFormat("E");
    // Calendar calendar = Calendar.getInstance();
    // CronExpressionEntry[] monthlyCrons;
    // CronExpressionEntry[] weeklyCrons;
    //
    // monthlyCrons = new CronExpressionEntry[31];
    // for (int i = 1; i <= 13; i++) {
    // monthlyCrons[i - 1] = new CronExpressionEntry(Integer.toString(i),
    // String.format(
    // mMonthlyCronPattern, Integer.toString(i)));
    // }
    //
    // weeklyCrons = new CronExpressionEntry[7];
    // for (int i = 1; i <= 7; i++) {
    // calendar.set(Calendar.DAY_OF_WEEK, i);
    // weeklyCrons[i - 1] = new CronExpressionEntry(dayFormatter
    // .format(calendar.getTime()), String.format(mWeeklyCronPattern, Integer
    // .toString(i)));
    // }
    //
    // mChildren = new CronExpressionEntry[][] { monthlyCrons, weeklyCrons };
    // }
    //
    // public Object getChild(int groupPosition, int childPosition) {
    // return mChildren[groupPosition][childPosition];
    // }
    //
    // public long getChildId(int groupPosition, int childPosition) {
    // return childPosition;
    // }
    //
    // public int getChildrenCount(int groupPosition) {
    // return mChildren[groupPosition].length;
    // }
    //
    // public TextView getGenericView() {
    // // Layout parameters for the ExpandableListView
    // AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
    // ViewGroup.LayoutParams.FILL_PARENT, 64);
    //
    // TextView textView = new TextView(CronPicker.this.getContext());
    // textView.setLayoutParams(lp);
    // // Center the text vertically
    // textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
    // // Set the text starting position
    // textView.setPadding(36, 0, 0, 0);
    // return textView;
    // }
    //
    // public View getChildView(int groupPosition, int childPosition, boolean
    // isLastChild,
    // View convertView, ViewGroup parent) {
    // TextView textView = getGenericView();
    // textView.setText(getChild(groupPosition, childPosition).toString());
    // return textView;
    // }
    //
    // public Object getGroup(int groupPosition) {
    // return mGroups[groupPosition];
    // }
    //
    // public int getGroupCount() {
    // return mGroups.length;
    // }
    //
    // public long getGroupId(int groupPosition) {
    // return groupPosition;
    // }
    //
    // public View getGroupView(int groupPosition, boolean isExpanded, View
    // convertView,
    // ViewGroup parent) {
    // TextView textView = getGenericView();
    // textView.setText(getGroup(groupPosition).toString());
    // return textView;
    // }
    //
    // public boolean isChildSelectable(int groupPosition, int childPosition) {
    // return true;
    // }
    //
    // public boolean hasStableIds() {
    // return true;
    // }
    //
    // }

    // /**
    // * This class will be used by the spinner, so the user can select from
    // * predefined cron expressions.
    // *
    // * It is designed immutable, so we can share instances of it between
    // * threads.
    // *
    // * This class is thread safe.
    // *
    // * @author lorenz fischer
    // */
    // private static class CronExpressionEntry {
    // /**
    // * The cron expression. <code>null</code> means not scheduled for reset.
    // */
    // private final String cronExpression;
    //
    // /**
    // * This will be printed inside the spinner through the
    // * {@link #toString()} method.
    // */
    // private final String label;
    //
    // /**
    // * Creates an entry that can be rendered inside the spinner using the
    // * {@link ArrayAdapter} class.
    // *
    // * @param label
    // * the label to render inside the spinner.
    // * @param cronExpression
    // * the cron expression the label stands for.
    // */
    // public CronExpressionEntry(String label, String cronExpression) {
    // this.label = label;
    // this.cronExpression = cronExpression;
    // }
    //
    // /**
    // * You can call this method on an entry in order to get the proper cron
    // * expression that the user has selected.
    // *
    // * @return the cron expression this entry stands for.
    // */
    // public String getCronExpression() {
    // return this.cronExpression;
    // }
    //
    // /**
    // * This method will be called by the {@link ArrayAdapter} in order to
    // * render it inside the spinner.
    // */
    // @Override
    // public String toString() {
    // return this.label;
    // }
    // }

}

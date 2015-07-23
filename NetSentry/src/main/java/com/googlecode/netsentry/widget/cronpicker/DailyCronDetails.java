/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.cronpicker;

import android.view.View;

import com.googlecode.netsentry.widget.listener.OnValueChangedListener;

/**
 * This cron details instance is used when the user selects a daily interval. It
 * does not provide a detail view or support any updates. The returned cron
 * expression is static.
 * 
 * @author lorenz fischer
 * 
 */
public class DailyCronDetails implements CronDetails {

    /**
     * If a given cron expression matches this pattern, the cron expression
     * stands for "every day".
     */
    public static final String CRON_EXPRESSION_PATTERN = "0 0 0 \\* \\* \\? \\*";

    /** The cron expression for "every day". */
    public static final String CRON_EXPRESSION = "0 0 0 * * ? *";

    /** {@inheritDoc} */
    @Override
    public String getCronExpression() {
        return CRON_EXPRESSION;
    }

    /** {@inheritDoc} */
    @Override
    public View getView() {
        return null; // we don't need to provide a view here.
    }

    /** {@inheritDoc} */
    @Override
    public void setCronExpression(String cronExpression) {
        // do nothing. there is only one cron expression for "every day"
    }

    /** {@inheritDoc} */
    @Override
    public void setOnValueChangedListener(OnValueChangedListener<String> callBack) {
        // since we don't allow changes, we also do not need to support change
        // listeners
    }

}

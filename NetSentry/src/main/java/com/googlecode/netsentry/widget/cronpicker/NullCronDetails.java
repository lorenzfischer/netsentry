/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.cronpicker;

import android.view.View;

import com.googlecode.netsentry.widget.listener.OnValueChangedListener;

/**
 * This cron detais instance always returns null for the currently selected cron
 * expression. It also does not support any changes or notifications.
 * 
 * @author lorenz fischer
 */
public class NullCronDetails implements CronDetails {

    @Override
    public String getCronExpression() {
        return null;
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void setCronExpression(String cronExpression) {
    }

    @Override
    public void setOnValueChangedListener(OnValueChangedListener<String> callBack) {
    }

}

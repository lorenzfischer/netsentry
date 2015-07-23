/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.cronpicker;

import com.googlecode.netsentry.widget.listener.OnValueChangedListener;

import android.view.View;

/**
 * This interface is used by the {@link CronPicker} in order to let the user
 * further specify the cron expression by supplying more detailed information.
 * 
 * @author lorenz fischer
 */
public interface CronDetails {

    /**
     * Sets all the components in this details picker to reflect the given
     * <code>cronExpression</code>. Calling this method will <b>not</b> invoke
     * any callbacks.
     * 
     * @param cronExpression
     *            the cron expression to select.
     */
    public void setCronExpression(String cronExpression);

    /**
     * This method returns the {@link View} instance that is rendering the
     * components for this details picker to the user.
     * 
     * @return an instance of {@link View} that will be added to the
     *         {@link CronPicker} component.
     */
    public View getView();
    
    /**
     * @return the cron expression currently being represented by this details picker.
     */
    public String getCronExpression();
    
    /**
     * Register a listener which will be informed about changes to the currently
     * set cron expression. Call this method with a <code>null</code> value in order
     * to de-register a previously registered listener.
     * 
     * @param callBack
     *            the listener instance.
     */
    public void setOnValueChangedListener(OnValueChangedListener<String> callBack);

}

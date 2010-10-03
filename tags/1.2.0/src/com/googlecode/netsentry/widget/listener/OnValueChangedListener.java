/**
 * TODO licence
 */
package com.googlecode.netsentry.widget.listener;

/**
 * A very generic interface for all sorts of changed listeners. These can be
 * used to tell a calling code block that a value (for example inside a dialog)
 * has changed.
 * 
 * @author lorenz fischer
 */
public interface OnValueChangedListener<T> {

    /**
     * @param oldValue
     *            the old number of bytes selected by this data picker.
     * @param newValue
     *            the new number of bytes selected by this data picker.
     */
    void onChanged(T oldValue, T newValue);

}

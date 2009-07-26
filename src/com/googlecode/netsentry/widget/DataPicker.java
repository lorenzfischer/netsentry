package com.googlecode.netsentry.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.googlecode.netsentry.R;
import com.googlecode.netsentry.widget.NumberPicker.OnChangedListener;

/**
 * A view for selecting an amount of data in Bytes, Megabytes, Gigabytes, or
 * Terrabytes.
 * 
 * @author lorenz fischer
 */
public class DataPicker extends FrameLayout {

    // constants
    public static final int UNIT_B = 0;
    public static final int UNIT_KB = 1;
    public static final int UNIT_MB = 2;
    public static final int UNIT_GB = 3;
    public static final int UNIT_TB = 4;

    private static final String[] UNITS = new String[] { "B", "KB", "MB", "GB", "TB" };

    /** The number picker for the amount value. */
    private final NumberPicker mAmountPicker;

    /** The picker for the unit value. */
    private final NumberPicker mUnitPicker;

    /** The currently selected number of bytes selected by this data picker. */
    private long mBytes = 0L;

    /** How we notify users the date has changed. */
    private OnBytesChangedListener mOnBytesChangedListener;

    /**
     * The callback used to indicate the user changes the number of bytes.
     */
    public interface OnBytesChangedListener {

        /**
         * @param view
         *            The view associated with this listener.
         * @param oldValue
         *            the old number of bytes selected by this data picker.
         * @param newValue
         *            the new number of bytes selected by this data picker.
         */
        void onChanged(DataPicker view, long oldValue, long newValue);
    }

    /** The listener we use to listen to the two number pickers. */
    private final OnChangedListener mPickerListener = new NumberPicker.OnChangedListener() {
        public void onChanged(NumberPicker picker, int oldVal, int newVal) {
            long oldValue = DataPicker.this.mBytes;
            updateBytes();
            if (mOnBytesChangedListener != null) {
                mOnBytesChangedListener
                        .onChanged(DataPicker.this, oldValue, DataPicker.this.mBytes);
            }
        }
    };

    /**
     * Constructor.
     * 
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public DataPicker(Context context) {
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
    public DataPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor.
     * 
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     * @param defStyle
     *            The default style to apply to this view. If 0, no style will
     *            be applied (beyond what is included in the theme). This may
     *            either be an attribute resource, whose value will be retrieved
     *            from the current theme, or an explicit style resource.
     */
    public DataPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.data_picker, this, // we are the parent
                true);

        mAmountPicker = (NumberPicker) findViewById(R.id.data_picker_amount);
        mAmountPicker.setSpeed(100);
        mAmountPicker.setRange(0, 1023);
        mAmountPicker.setOnChangeListener(mPickerListener);

        mUnitPicker = (NumberPicker) findViewById(R.id.data_picker_unit);
        // mAmountPicker.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
        mUnitPicker.setSpeed(100);
        mUnitPicker.setRange(0, UNITS.length - 1, UNITS);
        mUnitPicker.setOnChangeListener(mPickerListener);

        if (!isEnabled()) {
            setEnabled(false);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mAmountPicker.setEnabled(enabled);
        mUnitPicker.setEnabled(enabled);
    }

    /**
     * Computes the new number of bytes for the data picker using the current
     * <code>amount</code> and <code>unit</code>.
     * 
     * TODO write unit test for this
     */
    private void updateBytes() {
        long currentAmount = mAmountPicker.getCurrent();
        mBytes = currentAmount << (mUnitPicker.getCurrent() * 10);
    }

    /**
     * Returns the current number of bytes currently selected.
     * 
     * @return the currently selected amount of data in bytes.
     */
    public long getBytes() {
        return mBytes;
    }

    /**
     * @param amount
     *            amount value.
     * 
     * @param unit
     *            one of:
     *            <ul>
     *            <li>{@link #UNIT_B}</li>
     *            <li>{@link #UNIT_KB}</li>
     *            <li>{@link #UNIT_MB}</li>
     *            <li>{@link #UNIT_GB}</li>
     *            <li>{@link #UNIT_TB}</li>
     *            </ul>
     *            .
     */
    public void setData(int amount, int unit) {
        mAmountPicker.setCurrent(amount);
    }

    public int getAmount() {
        return mAmountPicker.getCurrent();
    }

    /**
     * Init the component.
     * 
     * @param bytes
     *            the amount of bytes to use for the initialization.
     * @param callBack
     *            this listener's onChanged() will be called when the user
     *            changes the value.
     */
    public void init(long bytes, OnBytesChangedListener callBack) {
        long amount = bytes;
        int unit = 0;

        while (amount > 1024) {
            unit++;
            amount >>= 10;
        }

        mAmountPicker.setCurrent((int) amount);
        mUnitPicker.setCurrent(unit);
        mOnBytesChangedListener = callBack;
    }

    /**
     * Override so we are in complete control of save / restore for this widget.
     */
    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        return new SavedState(superState, mAmountPicker.getCurrent(), mUnitPicker.getCurrent());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedSate = (SavedState) state;
        super.onRestoreInstanceState(savedSate.getSuperState());
        mAmountPicker.setCurrent(savedSate.getAmount());
        mUnitPicker.setCurrent(savedSate.getUnit());
    }

    /** This class is used for saving the current state of the view. */
    private static class SavedState extends BaseSavedState {

        private final int mAmount;
        private final int mUnit;

        /**
         * Constructor called from {@link DataPicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int amount, int unit) {
            super(superState);
            mAmount = amount;
            mUnit = unit;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mAmount = in.readInt();
            mUnit = in.readInt();
        }

        public int getAmount() {
            return mAmount;
        }

        public int getUnit() {
            return mUnit;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mAmount);
            dest.writeInt(mUnit);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
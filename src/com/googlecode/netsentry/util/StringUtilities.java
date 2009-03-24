/**
 * 
 */
package com.googlecode.netsentry.util;

import com.googlecode.netsentry.widget.DataPicker;

/**
 * This utility class contains methods that can be used for string manipulation.
 * There is no public constructor and the class is also defined as final. The
 * methods are all declared static.
 * 
 * @author lorenz fischer
 */
public final class StringUtilities {

    /**
     * These scales will be used to format byte numbers into human readable
     * forms. TODO the same stuff we also have in {@link DataPicker}
     */
    private static final String[] BYTE_SCALES = { "B", "KB", "MB", "GB", "TB" };

    /** Utility classes have private constructors. */
    private StringUtilities() {
    }

    /**
     * <p>
     * This method computes a String representing the amount of
     * <code>bytes</code> in a "human readable form".
     * </p>
     * 
     * <p>
     * <b>Examples:</br>
     * <ul>
     * <li>123 will be converted to "123 B"</li>
     * <li>1024 will be converted to "1 KB"</li>
     * <li>1048576 (1024*1024) will be converted to "1 MB"</li>
     * <li>1073741824 (1024*1024*1024) will be converted to "1 GB"</li>
     * <li>1099511627776 (1024*1024*1024*1024) will be converted to "1 TB"</li>
     * </ul>
     * </p>
     * 
     * The string will always be rounded to the biggest possible scale. So 1025
     * bytes will also be rendered as "1 KB" instead of "1 KB 1 B"
     * 
     * @param bytes
     *            the original number.
     * @return the human readable string representation.
     */
    public static String formatDataNumber(long bytes) {
        int counter = 0;

        while ((counter < StringUtilities.BYTE_SCALES.length - 1) && (bytes >>> 10) > 0) {
            bytes >>= 10; // unsigned is ok, since we never go below zero
            counter++;
        }

        return bytes + " " + StringUtilities.BYTE_SCALES[counter];

    }
}

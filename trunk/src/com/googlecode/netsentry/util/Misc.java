/**
 * TODO licence
 */
package com.googlecode.netsentry.util;

/**
 * This class holds miscellaneous utility methods.
 * 
 * @author lorenz fischer
 */
public class Misc {

    /**
     * Tests two objects for equality. If both are <code>null</code> this is
     * consiedered to be equal.
     * 
     * @param <T>
     * @param object1
     *            the one object.
     * @param object2
     *            the other object.
     * @return <code>true</code> if the two objects are equal, false otherwise.
     */
    public static <T> boolean areEqual(T object1, T object2) {
        if (object1 == object2) { // null == null
            return true;
        }
        if (object1 != null && object1.equals(object2)) {
            return true;
        }
        return false;
    }

	public static String formatDayOfMonth(int dayOfMonth) {
		int d1 = (int) Math.floor(dayOfMonth / 10);
		int d0 = dayOfMonth % 10;
		String suffix = "th";
		if (d1 != 1) {
			if (d0 == 1) {
				suffix = "st";
			} else if (d0 == 2) {
				suffix = "nd";
			} else if (d0 == 3) {
				suffix = "rd";
			}
		}
		return Integer.toString(dayOfMonth) + suffix;

	}

}

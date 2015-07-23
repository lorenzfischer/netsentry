/**
 * TODO licence
 */
package com.googlecode.netsentry.util;

import android.util.Log;

/**
 * Methods and global constants for logging.
 * @author lorenz fischer
 */
public final class LogUtils {

    /** Constant that defines the log level at which NetSentry should log messages. */
    private static final int LOG_LEVEL = Log.INFO;
    
    /** True if NetSentry should log on level {@link Log#VERBOSE}. */
    public static final boolean VERBOSE = LOG_LEVEL <= Log.VERBOSE;

    /** True if NetSentry should log on level {@link Log#DEBUG}. */
    public static final boolean DEBUG = LOG_LEVEL <= Log.DEBUG;

    /** True if NetSentry should log on level {@link Log#INFO}. */
    public static final boolean INFO = LOG_LEVEL <= Log.INFO;

    /** True if NetSentry should log on level {@link Log#WARN}. */
    public static final boolean WARN = LOG_LEVEL <= Log.WARN;

    /** True if NetSentry should log on level {@link Log#ERROR}. */
    public static final boolean ERROR = LOG_LEVEL <= Log.ERROR;

    /** True if NetSentry should log on level {@link Log#ASSERT}. */
    public static final boolean ASSERT = LOG_LEVEL <= Log.ASSERT;
    
}

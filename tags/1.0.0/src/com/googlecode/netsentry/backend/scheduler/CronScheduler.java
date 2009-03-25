/**
 * 
 */
package com.googlecode.netsentry.backend.scheduler;

import java.text.ParseException;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This broadcast receiver takes action only if it receives is of type
 * {@value #ACTION_EXECUTE_SCHEDULED_ACTION}: Whenever an intent with this
 * action name is received, this scheduler searches for an instance of
 * {@link Intent} in the extra field with name
 * {@value #EXTRA_FIELD_TARGET_INTENT} of the intent that this receiver was
 * called upon. If found, the scheduler will then broadcast this target intent
 * over current context and then re-schedule the target intent using the
 * {@link AlarmManager} of android. The date at which the intent will be
 * re-scheduled is computed from a cron expression that should be stored in the
 * extra string field with name {@value #EXTRA_FIELD_CRON_EXPRESSION}
 * 
 * @author lorenz fischer
 */
public class CronScheduler extends BroadcastReceiver {

    // /** The tag information for the logging facility. */
    // private static final String TAG = "ns.CronScheduler";

    /**
     * When an intent is received by this receiver, its extra information will
     * be searched for a asdf�ljasdflj with this name, so we know what kind of
     * intent should be fired from this receiver.
     */
    public static final String EXTRA_FIELD_TARGET_INTENT = "target.intent";

    /**
     * When an intent is received by this receiver, its extra information will
     * be searched for a string with this name, so we know when to re-schedule
     * the next execution.
     */
    public static final String EXTRA_FIELD_CRON_EXPRESSION = "cron.expression";

    /**
     * Whenever someone wants to schedule an intent for future broadcasting it
     * can wrap it inside an intent with this action name.
     */
    public static final String ACTION_EXECUTE_SCHEDULED_ACTION = "com.googlecode.netsentry.ACTION_EXECUTE_SCHEDULED_ACTION";

    /** {@inheritDoc} */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (CronScheduler.ACTION_EXECUTE_SCHEDULED_ACTION.equals(action)) {
            Intent targetIntent = intent
                    .getParcelableExtra(CronScheduler.EXTRA_FIELD_TARGET_INTENT);

            if (targetIntent != null) {
                String cronExpression = intent
                        .getStringExtra(CronScheduler.EXTRA_FIELD_CRON_EXPRESSION);

                // Log.d(TAG, "Executing scheduled job " +
                // targetIntent.getAction() + " with data "
                // + targetIntent.getDataString());

                // broadcast the wrapped intent
                context.sendBroadcast(targetIntent);

                // re-schedule
                scheduleJob(context, targetIntent, cronExpression);
            }
        }
    }

    /**
     * Computes the next execution time according to the
     * <code>cronExpression</code> used to create this
     * {@link CronScheduledIntent}. The relevant time for the computation is the
     * current system time.
     * 
     * @param cronExpression
     *            the cron expression to use for the computation.
     * @return a long value representing the time for the next execution in UTC.
     * @throws ParseException
     *             if the cron expression string could not be parsed.
     */
    private static Long getNextExecutionTime(String cronExpression) throws ParseException {
        return new CronExpression(cronExpression).getNextValidTimeAfter(new Date()).getTime();
    }

    /**
     * Creates an intent that will be repeatedly scheduled using the
     * {@link CronScheduler}.
     * 
     * @param targetIntent
     *            the intent to broad cast in every execution step.
     * @param cronExpression
     *            the cron expression to use for the scheduling.
     * @return an intent that is compatible with the {@link CronScheduler}.
     */
    private static Intent createCronScheduledIntent(Intent targetIntent, String cronExpression) {
        Intent result = new Intent();

        result.setAction(CronScheduler.ACTION_EXECUTE_SCHEDULED_ACTION);
        result.putExtra(CronScheduler.EXTRA_FIELD_TARGET_INTENT, targetIntent);
        result.putExtra(CronScheduler.EXTRA_FIELD_CRON_EXPRESSION, cronExpression);

        /*
         * We also need to set the data of the scheduled intent because the
         * Intent.filterEquals() only compares the action, data, type, class,
         * and categories of a scheduled broadcast intent. Since we don't want
         * to stop all scheduled intents but only the one's that are scheduled
         * for the same data record, we add it to this intent.
         */
        result.setData(targetIntent.getData());

        return result;
    }

    /**
     * Starts the scheduling process for <code>targetIntent</code> using
     * <code>cronExpression</code> for the scheduling and <code>context</code>
     * in order to broadcast the intent on every execution.
     * 
     * @param context
     *            the context to broadcast the <code>targetIntent</code> on.
     * @param targetIntent
     *            the intent to broadcast.
     * @param cronExpression
     *            this expression is used to compute subsequent executions.
     */
    public static void scheduleJob(Context context, Intent targetIntent, String cronExpression) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Log.d(TAG, "Scheduling job " + targetIntent.getAction() +
        // " with data "
        // + targetIntent.getDataString() + " for execution at: " +
        // cronExpression);

        try {
            alarmManager.set(AlarmManager.RTC, CronScheduler.getNextExecutionTime(cronExpression),
                    PendingIntent.getBroadcast(context, 0, CronScheduler.createCronScheduledIntent(
                            targetIntent, cronExpression), PendingIntent.FLAG_CANCEL_CURRENT));
        } catch (ParseException e) {
            // Log.e(TAG, "Could not parse cron expression '" + cronExpression +
            // "'. The intent "
            // + targetIntent.getAction() +
            // " will be not be scheduled for future execution.",
            // e);
        }
    }

    /**
     * Stops a job that has been scheduled for future execution from being
     * broadcasted.
     * 
     * @param context
     *            the context to broadcast the <code>targetIntent</code> on.
     * @param targetIntent
     *            the intent to broadcast.
     */
    public static void stopScheduledJob(Context context, Intent targetIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Log.d(TAG, "Stopping scheduled job " + targetIntent.getAction() +
        // " with data "
        // + targetIntent.getDataString());

        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, CronScheduler
                .createCronScheduledIntent(targetIntent, null), PendingIntent.FLAG_CANCEL_CURRENT));
    }

}

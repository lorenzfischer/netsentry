/**
 * 
 */
package com.googlecode.netsentry.backend.scheduler;

import java.util.Calendar;
import java.util.Date;

import com.googlecode.netsentry.util.CronExpression;

import junit.framework.TestCase;

/**
 * Tests the cron expressions we use.
 * 
 * @author lorenz fischer
 */
public class CronExpressionTest extends TestCase {

    /**
     * Tests the cron expression for "every Minute".
     */
    public void testTenSeconds() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Date startDate;
        Date endDate;
        CronExpression exp = new CronExpression("0/10 * * * * ? *");

        calendar.set(2009, 0, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        startDate = calendar.getTime();
        endDate = exp.getNextValidTimeAfter(startDate);
        calendar.add(Calendar.SECOND, 10);

        assertEquals(calendar.getTime(), endDate);
    }

    /**
     * Tests the cron expression for "every Minute".
     */
    public void testMinute() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Date startDate;
        Date endDate;
        CronExpression exp = new CronExpression("0 * * * * ? *");

        calendar.set(2009, 0, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        startDate = calendar.getTime();
        endDate = exp.getNextValidTimeAfter(startDate);
        calendar.add(Calendar.MINUTE, 1);

        assertEquals(calendar.getTime(), endDate);
    }

    /**
     * Tests the cron expression for "every Minute".
     */
    public void testEveryDay() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Date startDate;
        Date endDate;
        CronExpression exp = new CronExpression("0 0 0 * * ? *");

        calendar.set(2009, 0, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        startDate = calendar.getTime();
        endDate = exp.getNextValidTimeAfter(startDate);
        calendar.add(Calendar.DATE, 1);

        assertEquals(calendar.getTime(), endDate);
    }

    /**
     * Tests the cron expression for "every Minute".
     */
    public void testEveryMonth() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Date startDate;
        Date endDate;
        CronExpression exp = new CronExpression("0 0 0 1 * ? *");

        calendar.set(2009, 1, 5, 10, 20, 35); // set arbitrary date and time
        calendar.set(Calendar.MILLISECOND, 0);
        startDate = calendar.getTime();
        endDate = exp.getNextValidTimeAfter(startDate);

        // compute end date using add() method
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.MONTH, 1);

        assertEquals(calendar.getTime(), endDate);
    }

}

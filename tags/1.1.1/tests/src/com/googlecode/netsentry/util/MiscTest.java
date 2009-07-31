package com.googlecode.netsentry.util;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Tests for the utility functions in {@link Misc}.
 * 
 * @author lorenz fischer
 */
public class MiscTest extends TestCase {

    public void testAreEqual() throws Exception {
        String str1 = String.valueOf(new char[] { 'a', 'b', 'c' });
        String str2 = String.valueOf(new char[] { 'a', 'b', 'c' });
        Assert.assertTrue(Misc.areEqual(str1, str2));
    }

    public void testAreEqualNull() throws Exception {
        Assert.assertTrue(Misc.areEqual(null, null));
    }

    public void testAreEqualOneNull() throws Exception {
        Assert.assertTrue(null == null);
        Assert.assertFalse(Misc.areEqual(null, "asdf"));
        Assert.assertFalse(Misc.areEqual("asdf", null));
    }

}

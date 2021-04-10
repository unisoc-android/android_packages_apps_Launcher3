package com.android.launcher3.logging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.util.Scheduler;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

/**
 * Tests for {@link FileLog}
 */
@RunWith(RobolectricTestRunner.class)
public class FileLogTest {

    private File mTempDir;
    private boolean mTestActive;

    @Before
    public void setUp() throws Exception {
        int count = 0;
        do {
            mTempDir = new File(RuntimeEnvironment.application.getCacheDir(),
                    "log-test-" + (count++));
        } while (!mTempDir.mkdir());

        FileLog.setDir(mTempDir);

        mTestActive = true;
        Scheduler scheduler = Shadows.shadowOf(FileLog.getHandler().getLooper()).getScheduler();
        new Thread(() -> {
            while (mTestActive) {
                scheduler.advanceToLastPostedRunnable();
            }
        }).start();
    }

    @After
    public void tearDown() throws Exception {
        // Clear existing logs
        new File(mTempDir, "log-0").delete();
        new File(mTempDir, "log-1").delete();
        mTempDir.delete();

        mTestActive = false;
    }

    @Test
    public void testPrintLog() throws Exception {
        if (!FileLog.ENABLED) {
            return;
        }
        FileLog.print("Testing", "hoolalala");
        StringWriter writer = new StringWriter();
        FileLog.flushAll(new PrintWriter(writer));
        assertTrue(writer.toString().contains("hoolalala"));

        FileLog.print("Testing", "abracadabra", new Exception("cat! cat!"));
        writer = new StringWriter();
        FileLog.flushAll(new PrintWriter(writer));
        assertTrue(writer.toString().contains("abracadabra"));
        // Exception is also printed
        assertTrue(writer.toString().contains("cat! cat!"));

        // Old logs still present after flush
        assertTrue(writer.toString().contains("hoolalala"));
    }

    @Test
    public void testOldFileTruncated() throws Exception {
        if (!FileLog.ENABLED) {
            return;
        }
        FileLog.print("Testing", "hoolalala");
        StringWriter writer = new StringWriter();
        FileLog.flushAll(new PrintWriter(writer));
        assertTrue(writer.toString().contains("hoolalala"));

        Calendar threeDaysAgo = Calendar.getInstance();
        threeDaysAgo.add(Calendar.HOUR, -72);
        new File(mTempDir, "log-0").setLastModified(threeDaysAgo.getTimeInMillis());
        new File(mTempDir, "log-1").setLastModified(threeDaysAgo.getTimeInMillis());

        FileLog.print("Testing", "abracadabra", new Exception("cat! cat!"));
        writer = new StringWriter();
        FileLog.flushAll(new PrintWriter(writer));
        assertTrue(writer.toString().contains("abracadabra"));
        // Exception is also printed
        assertTrue(writer.toString().contains("cat! cat!"));

        // Old logs have been truncated
        assertFalse(writer.toString().contains("hoolalala"));
    }
}

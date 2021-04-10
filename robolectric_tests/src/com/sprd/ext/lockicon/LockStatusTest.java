package com.sprd.ext.lockicon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import com.android.systemui.shared.recents.model.Task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LockStatusTest {
    private static final String TEST_PACKAGENAME = "com.android.test";
    private static final String TEST_ACTIVITYNAME = "com.android.test";
    private static final String TEST_TASK_AFFINITY = "testAffinity";

    @Test
    public void testStringKey() throws Exception {
        Context context = mock(Context.class);
        Task task = mock(Task.class);
        Intent intent = mock(Intent.class);
        Task.TaskKey key = new Task.TaskKey(0, 0, intent, null, 0, 0);
        task.key = key;
        ComponentName componentName = new ComponentName(TEST_PACKAGENAME, TEST_ACTIVITYNAME);
        ActivityInfo info = new ActivityInfo();
        PackageManager packageManager = mock(PackageManager.class);

        when(context.getPackageManager()).thenReturn(packageManager);
        when(intent.getComponent()).thenReturn(componentName);
        when(packageManager.getActivityInfo(eq(componentName), anyInt())).thenReturn(info);

        info.taskAffinity = TEST_TASK_AFFINITY;
        assertTrue("lock after setLockState true",
                TaskLockStatus.makeTaskStringKey(context, task).contains(TEST_TASK_AFFINITY));

        info.taskAffinity = null;
        assertTrue("lock after setLockState true",
                TaskLockStatus.makeTaskStringKey(context, task).contains(TEST_PACKAGENAME));
        assertTrue("lock after setLockState true",
                TaskLockStatus.makeTaskStringKey(context, task).contains(TEST_ACTIVITYNAME));
    }

    @Test
    public void testLockStatus() {
        Context context = RuntimeEnvironment.application;
        assertFalse("unlock",
                TaskLockStatus.isSavedLockedTask(context, TEST_TASK_AFFINITY));
        TaskLockStatus.setLockState(context, TEST_TASK_AFFINITY, true);
        assertTrue("lock after setLockState true",
                TaskLockStatus.isSavedLockedTask(context, TEST_TASK_AFFINITY));
        TaskLockStatus.setLockState(context, TEST_TASK_AFFINITY, false);
        assertFalse("unlock after setLockState false",
                TaskLockStatus.isSavedLockedTask(context, TEST_TASK_AFFINITY));
    }
}

package com.sprd.ext.clearall;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.android.launcher3.LauncherState.BACKGROUND_APP;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.OVERVIEW;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ClearAllControllerTest {
    private ClearAllButton mClearAllButton = mock(ClearAllButton.class);
    private ClearAllController mClearAllController;

    @Before
    public void setUp() {
        // use real content alpha method for testing state switching
        doCallRealMethod().when(mClearAllButton).setContentAlpha(anyFloat());
        doCallRealMethod().when(mClearAllButton).getContentAlpha();
        mClearAllController = new ClearAllController(mClearAllButton);
    }

    @Test
    public void testReceiveStateTransitionStartWhenHasTasks() {
        //only OVERVIEW do nothing, other state change alpha to 0f
        mClearAllController.onTaskStackUpdated(true);
        mClearAllButton.setContentAlpha(0f);
        mClearAllController.onStateTransitionStart(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionStart(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionStart(OVERVIEW);
        finishAmin();
        assertEquals("alpha is still 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionStart(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionStart(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionStart(OVERVIEW);
        finishAmin();
        assertEquals("alpha is still 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
    }

    @Test
    public void testReceiveStateTransitionCompleteWhenHasTasks() {
        //only OVERVIEW change alpha to 1f, other state change alpha to 0f
        mClearAllController.onTaskStackUpdated(true);
        mClearAllButton.setContentAlpha(0f);
        mClearAllController.onStateTransitionComplete(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionComplete(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionComplete(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionComplete(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);


        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionComplete(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionComplete(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
    }

    @Test
    public void testReceiveStateTransitionStartWhenNoTasks() {
        //only OVERVIEW do nothing, other state change alpha to 0f
        mClearAllController.onTaskStackUpdated(false);
        mClearAllButton.setContentAlpha(0f);
        mClearAllController.onStateTransitionStart(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionStart(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionStart(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionStart(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionStart(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionStart(OVERVIEW);
        finishAmin();
        assertEquals("alpha is still 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
    }

    @Test
    public void testReceiveStateTransitionCompleteWhenNoTasks() {
        //only OVERVIEW change alpha to 1f, other state change alpha to 0f
        mClearAllController.onTaskStackUpdated(false);
        mClearAllButton.setContentAlpha(0f);
        mClearAllController.onStateTransitionComplete(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionComplete(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionComplete(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllController.onStateTransitionComplete(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionComplete(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        mClearAllController.onStateTransitionComplete(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);
    }

    @Test
    public void testShowImmediately() {
        //change alpha to 1f if has tasks
        mClearAllButton.setContentAlpha(1f);
        assertEquals("Initialize alpha to 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.onTaskStackUpdated(false);
        mClearAllController.showImmediately();
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(0f);
        assertEquals("Initialize alpha to 0f", 0f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.onTaskStackUpdated(false);
        mClearAllController.showImmediately();
        finishAmin();
        assertEquals("alpha is still 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        assertEquals("Initialize alpha to 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.onTaskStackUpdated(true);
        mClearAllController.showImmediately();
        finishAmin();
        assertEquals("alpha is still 1f", 1f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(0f);
        assertEquals("Initialize alpha to 0f", 0f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.onTaskStackUpdated(true);
        mClearAllController.showImmediately();
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
    }

    @Test
    public void testTaskStackUpdated() {
        //after showImmediately or onStateTransitionComplete(OVERVIEW), set alpha to 1f
        mClearAllButton.setContentAlpha(0f);
        assertEquals("Initialize alpha to 0f", 0f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.onTaskStackUpdated(true);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(1f);
        assertEquals("Initialize alpha to 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.onTaskStackUpdated(true);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(0f);
        assertEquals("Initialize alpha to 0f", 0f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.showImmediately();
        mClearAllController.onTaskStackUpdated(true);
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mClearAllButton.getContentAlpha(), 0f);

        mClearAllButton.setContentAlpha(0f);
        assertEquals("Initialize alpha to 0f", 0f, mClearAllButton.getContentAlpha(), 0f);
        mClearAllController.onStateTransitionComplete(OVERVIEW);
        mClearAllController.onTaskStackUpdated(true);
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mClearAllButton.getContentAlpha(), 0f);
    }

    public void finishAmin() {
        if (mClearAllController.getAmin() != null) {
            mClearAllController.getAmin().end();
        }
    }
}

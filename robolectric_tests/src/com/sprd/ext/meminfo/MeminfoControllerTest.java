package com.sprd.ext.meminfo;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.android.launcher3.LauncherState.BACKGROUND_APP;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.OVERVIEW;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MeminfoControllerTest {
    private Launcher mLauncher = mock(Launcher.class);
    private MeminfoView mMeminfoView = mock(MeminfoView.class);
    private LauncherStateManager mLauncherStateManager = mock(LauncherStateManager.class);
    private MeminfoController mMeminfoController;

    @Before
    public void setUp() {
        when(mLauncher.getStateManager()).thenReturn(mLauncherStateManager);
        when(mLauncher.findViewById(R.id.recents_memoryinfo)).thenReturn(mMeminfoView);
        // use real content alpha method for testing state switching
        doCallRealMethod().when(mMeminfoView).setContentAlpha(anyFloat());
        doCallRealMethod().when(mMeminfoView).getContentAlpha();
        mMeminfoController = new MeminfoController(mLauncher);
    }

    @Test
    public void testRegisterStateListener() {
        verify(mLauncherStateManager, times(1)).addStateListener(anyObject());
    }

    @Test
    public void testReceiveStateTransitionStart() {
        //only OVERVIEW do nothing, other state change alpha to 0f
        mMeminfoView.setContentAlpha(0f);
        mMeminfoController.onStateTransitionStart(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoController.onStateTransitionStart(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoController.onStateTransitionStart(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoView.setContentAlpha(1f);
        mMeminfoController.onStateTransitionStart(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoView.setContentAlpha(1f);
        mMeminfoController.onStateTransitionStart(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoView.setContentAlpha(1f);
        mMeminfoController.onStateTransitionStart(OVERVIEW);
        finishAmin();
        assertEquals("alpha is still 1f", 1f, mMeminfoView.getContentAlpha(), 0f);
    }

    @Test
    public void testReceiveStateTransitionComplete() {
        //only OVERVIEW change alpha to 1f, other state change alpha to 0f
        mMeminfoView.setContentAlpha(0f);
        mMeminfoController.onStateTransitionComplete(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoController.onStateTransitionComplete(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoController.onStateTransitionComplete(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoController.onStateTransitionComplete(NORMAL);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoView.setContentAlpha(1f);
        mMeminfoController.onStateTransitionComplete(BACKGROUND_APP);
        finishAmin();
        assertEquals("alpha need be 0f", 0f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoView.setContentAlpha(1f);
        mMeminfoController.onStateTransitionComplete(OVERVIEW);
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mMeminfoView.getContentAlpha(), 0f);
    }

    @Test
    public void testShowImmediately() {
        //showImmediately, change alpha to 1f
        mMeminfoView.setContentAlpha(1f);
        assertEquals("Initialize alpha to 1f", 1f, mMeminfoView.getContentAlpha(), 0f);
        mMeminfoController.showImmediately();
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mMeminfoView.getContentAlpha(), 0f);

        mMeminfoView.setContentAlpha(0f);
        assertEquals("Initialize alpha to 0f", 0f, mMeminfoView.getContentAlpha(), 0f);
        mMeminfoController.showImmediately();
        finishAmin();
        assertEquals("alpha need be 1f", 1f, mMeminfoView.getContentAlpha(), 0f);
    }

    public void finishAmin() {
        if (mMeminfoController.getAmin() != null) {
            mMeminfoController.getAmin().end();
        }
    }
}

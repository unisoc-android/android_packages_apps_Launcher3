package com.sprd.ext.clearall;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ClearAllButtonTest {
    private ClearAllButton mClearAllButton = mock(ClearAllButton.class);

    @Test
    public void testContentAlpha() {
        doCallRealMethod().when(mClearAllButton).setContentAlpha(anyFloat());
        doCallRealMethod().when(mClearAllButton).getContentAlpha();

        when(mClearAllButton.getVisibility()).thenReturn(GONE);
        mClearAllButton.setContentAlpha(0f);
        assertEquals(0f, mClearAllButton.getContentAlpha(), 0f);
        verify(mClearAllButton, times(0)).setVisibility(GONE);

        when(mClearAllButton.getVisibility()).thenReturn(VISIBLE);
        mClearAllButton.setContentAlpha(0f);
        assertEquals(0f, mClearAllButton.getContentAlpha(), 0f);
        verify(mClearAllButton, times(1)).setVisibility(GONE);

        when(mClearAllButton.getVisibility()).thenReturn(VISIBLE);
        mClearAllButton.setContentAlpha(1f);
        assertEquals(1f, mClearAllButton.getContentAlpha(), 0f);
        verify(mClearAllButton, times(0)).setVisibility(VISIBLE);

        when(mClearAllButton.getVisibility()).thenReturn(GONE);
        mClearAllButton.setContentAlpha(1f);
        assertEquals(1f, mClearAllButton.getContentAlpha(), 0f);
        verify(mClearAllButton, times(1)).setVisibility(VISIBLE);
    }
}

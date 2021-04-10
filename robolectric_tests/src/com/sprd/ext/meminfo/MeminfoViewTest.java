package com.sprd.ext.meminfo;

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
public class MeminfoViewTest {
    private MeminfoView mMeminfoView = mock(MeminfoView.class);

    @Test
    public void testContentAlpha() {
        doCallRealMethod().when(mMeminfoView).setContentAlpha(anyFloat());
        doCallRealMethod().when(mMeminfoView).getContentAlpha();

        when(mMeminfoView.getVisibility()).thenReturn(GONE);
        mMeminfoView.setContentAlpha(0f);
        assertEquals(0f, mMeminfoView.getContentAlpha(), 0f);
        verify(mMeminfoView, times(0)).setVisibility(GONE);

        when(mMeminfoView.getVisibility()).thenReturn(VISIBLE);
        mMeminfoView.setContentAlpha(0f);
        assertEquals(0f, mMeminfoView.getContentAlpha(), 0f);
        verify(mMeminfoView, times(1)).setVisibility(GONE);

        when(mMeminfoView.getVisibility()).thenReturn(VISIBLE);
        mMeminfoView.setContentAlpha(1f);
        assertEquals(1f, mMeminfoView.getContentAlpha(), 0f);
        verify(mMeminfoView, times(0)).setVisibility(VISIBLE);

        when(mMeminfoView.getVisibility()).thenReturn(GONE);
        mMeminfoView.setContentAlpha(1f);
        assertEquals(1f, mMeminfoView.getContentAlpha(), 0f);
        verify(mMeminfoView, times(1)).setVisibility(VISIBLE);
    }
}

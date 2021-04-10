package com.sprd.ext.folder;

import android.content.Context;

import com.android.launcher3.R;
import com.android.launcher3.folder.PreviewItemDrawingParams;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GridFolderIconLayoutRuleTest {

    protected Context mTargetContext;
    private GridFolderIconLayoutRule mRule;

    private int mPreviewRows;
    private int mPreviewColumns;
    private float mScale;

    @Before
    public void setup(){
        ShadowLog.stream = System.out;
        mTargetContext = RuntimeEnvironment.application;

        mPreviewRows  = mTargetContext.getResources().getInteger(R.integer.grid_folder_icon_rows);
        mPreviewColumns = mTargetContext.getResources().getInteger(R.integer.grid_folder_icon_columns);
        mScale = mTargetContext.getResources().getInteger(
                R.integer.grid_folder_app_icon_size_percentage) / 100.0f;

        mRule = new GridFolderIconLayoutRule(mTargetContext);
        mRule.init(108, 108, false);
    }

    @Test
    public void testMaxNumItemsInPreview() {
        assertEquals(mPreviewRows * mPreviewColumns, mRule.getMaxNumItemsInPreview());
    }

    @Test
    public void testScaleForItem() {
        assertEquals(mScale, mRule.scaleForItem(0), 1.0f);
        assertEquals(mScale, mRule.scaleForItem(3), 1.0f);
        assertEquals(mScale, mRule.scaleForItem(8), 1.0f);
    }

    @Test
    public void testComputePreviewItemDrawingParams() {
        PreviewItemDrawingParams tempParams = new PreviewItemDrawingParams(0, 0, 0, 0);

        PreviewItemDrawingParams firstItemParams = mRule.computePreviewItemDrawingParams(
                0, 12, tempParams);
        PreviewItemDrawingParams lastItemParams = mRule.computePreviewItemDrawingParams(
                mRule.getMaxNumItemsInPreview() - 1, 12, tempParams);

        assertTrue(firstItemParams.getTransX() > mRule.getIconSize() * mScale );
        assertTrue( firstItemParams.getTransY() > mRule.getIconSize() * mScale);

        assertTrue(lastItemParams.getTransX() >= mRule.getIconSize() * mScale * (mPreviewColumns - 1));
        assertTrue(lastItemParams.getTransX() < mRule.getIconSize());

        assertTrue(lastItemParams.getTransY() >= mRule.getIconSize() * mScale * (mPreviewRows - 1));
        assertTrue( lastItemParams.getTransY() <= mRule.getIconSize());

        PreviewItemDrawingParams outOfMaxPreviewItemParams = mRule.computePreviewItemDrawingParams(
                mRule.getMaxNumItemsInPreview(), 12, tempParams);
        assertTrue(outOfMaxPreviewItemParams.getTransX() <= mRule.getIconSize() / 2);
        assertTrue(outOfMaxPreviewItemParams.getTransY() <= mRule.getIconSize() / 2);
    }
}

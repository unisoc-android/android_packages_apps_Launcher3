package com.sprd.ext.folder;

import android.content.Context;
import android.content.res.Resources;

import com.android.launcher3.R;
import com.android.launcher3.folder.ClippedFolderIconLayoutRule;
import com.android.launcher3.folder.PreviewItemDrawingParams;

/**
 * Created on 2019/5/20.
 */
public class GridFolderIconLayoutRule extends ClippedFolderIconLayoutRule {

    private int mGridCountX;
    private int mGridCountY;
    private float mItemIconScale;

    private final int mMaxNumItemsInPreview;

    GridFolderIconLayoutRule (Context context) {
        Resources resources = context.getResources();
        mGridCountX = resources.getInteger(R.integer.grid_folder_icon_rows);
        mGridCountY = resources.getInteger(R.integer.grid_folder_icon_columns);
        mItemIconScale = resources.getInteger(R.integer.grid_folder_app_icon_size_percentage) / 100.0f;

        mMaxNumItemsInPreview = mGridCountX * mGridCountY;
    }

    @Override
    public int getMaxNumItemsInPreview() {
        return mMaxNumItemsInPreview;
    }

    @Override
    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index, int curNumItems, PreviewItemDrawingParams params) {
        float transX;
        float transY;
        float overlayAlpha = 0;
        float iconSize = getIconSize();
        float scale = scaleForItem(index);

        float[] point = new float[2];

        if (index < mMaxNumItemsInPreview) {
            int baseX = index % mGridCountX;
            int baseY = index / mGridCountY;
            if (mIsRtl) {
                baseX = (mGridCountX - 1) - baseX;
            }

            float paddingX = (mAvailableSpace - (iconSize * scale) * mGridCountX) / (mGridCountX + 1);
            if (paddingX < 0) {
                paddingX = 0;
            }

            float paddingY = (mAvailableSpace - (iconSize * scale) * mGridCountY) / (mGridCountY + 1);
            if (paddingY < 0) {
                paddingY = 0;
            }

            point[0] = (baseX + 1) * paddingX + baseX * (iconSize * scale);
            point[1] = (baseY + 1) * paddingY + baseY * (iconSize * scale);
        } else {
            point[0] = point[1] = mAvailableSpace / 2 - (iconSize * scale) / 2;
        }

        transX = point[0];
        transY = point[1];

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, scale, overlayAlpha);
        } else {
            params.update(transX, transY, scale);
        }
        return params;
    }

    @Override
    public float scaleForItem(int numItems) {
        return mItemIconScale * mBaselineIconScale;
    }
}

package com.sprd.ext.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.android.launcher3.views.ClipPathView;

/**
 * Created on 2019/5/20.
 */
public class GridFolderPage extends FrameLayout implements ClipPathView {
    public Path mClipPath;

    public GridFolderPage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setClipPath(Path clipPath) {
        mClipPath = clipPath;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mClipPath != null) {
            int count = canvas.save();
            canvas.clipPath(mClipPath);
            super.draw(canvas);
            canvas.restoreToCount(count);
        } else {
            super.draw(canvas);
        }
    }
}

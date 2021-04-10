package com.sprd.ext.cyclescroll;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.PagedView;
import com.android.launcher3.pageindicators.PageIndicator;
import com.sprd.ext.LogUtils;

/**
 * An abstraction of the original Workspace which supports circular slide.
 */
public abstract class CyclePagedView<T extends View & PageIndicator> extends PagedView<T> {
    private static final String TAG = "CyclePagedView";
    private static final boolean DEBUG = LogUtils.DEBUG_ALL;

    protected static final int OVER_FIRST_PAGE_INDEX = -1;
    private static final int OVER_FIRST_INDEX = 0;
    private static final int OVER_LAST_INDEX = 1;

    public boolean mIsEnableCycle;
    private int[] mOverPageLeft = new int[]{0, 0};
    protected int mPageWidth = 0;

    public CyclePagedView(Context context) {
        this(context, null);
    }

    public CyclePagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CyclePagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Returns the index of page to be shown immediately afterwards.
     */
    @Override
    public int getNextPage() {
        int nextPage = mNextPage;
        if (enableLoop()) {
            if (isOverFirstPage(mNextPage)) {
                nextPage = getChildCount() - 1;
            } else if (isOverLastPage(mNextPage)) {
                nextPage = 0;
            }
        }
        return (mNextPage != INVALID_PAGE) ? nextPage : mCurrentPage;
    }

    @Override
    protected int validateNewPage(int newPage, boolean isSnapTo) {
        int validatedPage = newPage;
        if (enableLoop() && isSnapTo) {
            validatedPage = Math.max(OVER_FIRST_PAGE_INDEX, Math.min(validatedPage, getPageCount()));
        } else {
            // Ensure that it is clamped by the actual set of children in all cases
            validatedPage = super.validateNewPage(newPage, isSnapTo);
        }
        return validatedPage;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mPageScrolls != null && mPageScrolls.length > 1) {
            mPageWidth = Math.abs(mPageScrolls[1] - mPageScrolls[0]);
            mOverPageLeft[(mIsRtl ? OVER_LAST_INDEX : OVER_FIRST_INDEX)] = -mPageWidth;
            mOverPageLeft[(mIsRtl ? OVER_FIRST_INDEX : OVER_LAST_INDEX)] = mPageScrolls[mIsRtl ?
                    0 : (mPageScrolls.length - 1)] + mPageWidth;
        }
    }

    // Add for draw temp page
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (enableLoop()) {
            drawCircularPageIfNeed(canvas);
        }
    }

    @Override
    public int getScrollForPage(int index) {
        if (enableLoop()) {
            if (isOverFirstPage(index)) {
                return mOverPageLeft[OVER_FIRST_INDEX];
            } else if (isOverLastPage(index)) {
                return mOverPageLeft[OVER_LAST_INDEX];
            }
        }
        return super.getScrollForPage(index);
    }

    @Override
    protected boolean isXBeforeFirstPage(int x) {
        return !enableLoop() && super.isXBeforeFirstPage(x);
    }

    @Override
    protected boolean isXAfterLastPage(int x) {
        return !enableLoop() && super.isXAfterLastPage(x);
    }

    @Override
    protected int getMinPageIndex() {
        return enableLoop() ? OVER_FIRST_PAGE_INDEX : super.getMinPageIndex();
    }

    @Override
    protected int getMaxPageIndex() {
        return enableLoop() ? getChildCount() : super.getMaxPageIndex();
    }

    public boolean enableLoop() {
        boolean multPage = false;
        // Can't circular slide when there is only one page
        if (mPageScrolls != null) {
            multPage = mPageScrolls.length > 1;
        }
        return mIsEnableCycle && multPage;
    }

    @Override
    protected int validateCycleNewPage() {
        int currentPage;
        if (enableLoop()) {
            if (isOverFirstPage(mNextPage) && mPageScrolls != null) {
                currentPage = getPageCount() - 1;
            } else if (isOverLastPage(mNextPage) && mPageScrolls != null) {
                currentPage = 0;
            } else {
                currentPage = validateNewPage(mNextPage, false);
            }
            scrollTo(mPageScrolls[currentPage], getScrollY());
        } else {
            currentPage = super.validateCycleNewPage();
        }
        if (DEBUG) {
            LogUtils.d(TAG, "validate Cycle New Page currentPage:" + currentPage);
        }
        return currentPage;
    }

    @Override
    protected int computeTotalDistance(View v, int adjacentPage, int page) {
        int totalDistance;
        if (enableLoop() && (isOverFirstPage(adjacentPage) || isOverLastPage(adjacentPage))) {
            totalDistance = Math.abs(getScrollForPage(adjacentPage) - getScrollForPage(page));
        } else {
            totalDistance = super.computeTotalDistance(v, adjacentPage, page);
        }
        return totalDistance;
    }

    @Override
    protected int recomputeDelta(int delta, int screenCenter, int page, int totalDistance) {
        int index = 0;
        final int halfScreenSize = getMeasuredWidth() / 2;
        if (enableLoop()) {
            int overScrollX = getScrollX();
            if (mIsRtl) {
                if (overScrollX < 0 && page == 0) {
                    index = getChildCount();
                } else if (overScrollX > mMaxScrollX && page == getChildCount() - 1) {
                    index = OVER_FIRST_PAGE_INDEX;
                }
            } else {
                if (overScrollX > mMaxScrollX && page == 0) {
                    index = getChildCount();
                } else if (overScrollX < 0 && page == getChildCount() - 1) {
                    index = OVER_FIRST_PAGE_INDEX;
                }
            }
        }
        return (index == 0) ? delta : (screenCenter - (getScrollForPage(index) + halfScreenSize));
    }

    private void drawCircularPageIfNeed(Canvas canvas) {
        int overScrollX = getScrollX();
        boolean isXBeforeFirstPage = mIsRtl ? (overScrollX > mMaxScrollX) : (overScrollX < 0);
        boolean isXAfterLastPage = mIsRtl ? (overScrollX < 0) : (overScrollX > mMaxScrollX);
        if (isXBeforeFirstPage || isXAfterLastPage) {
            long drawingTime = getDrawingTime();
            int childCount = getChildCount();
            canvas.save();
            canvas.clipRect(getScrollX(), getScrollY(), getScrollX() + getRight() - getLeft(),
                    getScrollY() + getBottom() - getTop());
            // Here we assume that a page's horizontal padding plus it's measured width
            // equals to ViewPort's width
            int offset = (mIsRtl ? -childCount : childCount) * (mPageWidth);
            if (isXBeforeFirstPage) {
                canvas.translate(-offset, 0);
                drawChild(canvas, getPageAt(childCount - 1), drawingTime);
                canvas.translate(+offset, 0);
            } else {
                canvas.translate(+offset, 0);
                drawChild(canvas, getPageAt(0), drawingTime);
                canvas.translate(-offset, 0);
            }
            canvas.restore();
        }
    }

    public void setCycleSlideEnabled(boolean enabled) {
        mIsEnableCycle = enabled;
    }

    private boolean isOverLastPage(final int pageIndex) {
        return pageIndex == getPageCount();
    }

    private boolean isOverFirstPage(final int pageIndex) {
        return pageIndex == OVER_FIRST_PAGE_INDEX;
    }
}

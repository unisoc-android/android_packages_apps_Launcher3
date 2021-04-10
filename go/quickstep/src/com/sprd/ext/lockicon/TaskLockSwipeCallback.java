package com.sprd.ext.lockicon;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE;
import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;

import static com.android.quickstep.TaskAdapter.ITEM_TYPE_CLEAR_ALL;

import android.graphics.Canvas;

import com.android.launcher3.R;
import com.android.quickstep.TaskHolder;
import com.android.quickstep.views.TaskItemView;
import com.sprd.ext.LogUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Callback for swipe input on {@link TaskHolder} views in the recents view.
 */
public final class TaskLockSwipeCallback extends ItemTouchHelper.SimpleCallback {
    private static final String TAG = "TaskLockSwipeCallback";
    private static final float UNSWIPE_THRESHOLD = 2f;
    private float mMaxSwipeVelocity = 0;
    private final float mLockEndDisplacement;
    private boolean mNeedChangeLockStatus = false;

    public TaskLockSwipeCallback(float lockEndDisplacement) {
        super(0 /* dragDirs */, LEFT);
        mLockEndDisplacement = lockEndDisplacement;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(ViewHolder viewHolder, int direction) {
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "onSwiped direction: " + direction);
        }
        mNeedChangeLockStatus = false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull ViewHolder viewHolder, float dX, float dY, int actionState,
                            boolean isCurrentlyActive) {
        if (actionState == ACTION_STATE_SWIPE) {
            TaskItemView itemView = (TaskItemView) (viewHolder.itemView);
            float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth();
            itemView.setAlpha(alpha);
            if (isCurrentlyActive) {
                mNeedChangeLockStatus = dX < -mLockEndDisplacement;
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY,
                actionState, isCurrentlyActive);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == ITEM_TYPE_CLEAR_ALL) {
            // Clear all button should not be swipable.
            return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "clearView viewHolder: " + viewHolder);
        }
        TaskHolder taskHolder = (TaskHolder) viewHolder;
        if (mNeedChangeLockStatus && taskHolder != null) {
            taskHolder.getTaskItemView().changeLockState(recyclerView.getContext());
        }
        mNeedChangeLockStatus = false;
        super.clearView(recyclerView, viewHolder);
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return mMaxSwipeVelocity + 1;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        mMaxSwipeVelocity = super.getSwipeVelocityThreshold(defaultValue);
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "getSwipeThreshold mMaxSwipeVelocity: " + mMaxSwipeVelocity);
        }
        return mMaxSwipeVelocity;
    }

    @Override
    public float getSwipeThreshold(@NonNull ViewHolder viewHolder) {
        return UNSWIPE_THRESHOLD;
    }
}

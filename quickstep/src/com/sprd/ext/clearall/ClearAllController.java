package com.sprd.ext.clearall;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.util.FloatProperty;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.VisibleForTesting;

import static com.android.launcher3.anim.Interpolators.LINEAR;
import static com.android.launcher3.uioverrides.RecentsUiFactory.GO_LOW_RAM_RECENTS_ENABLED;

import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.R;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LogUtils;
import com.sprd.ext.meminfo.MeminfoController;

public class ClearAllController implements LauncherStateManager.StateListener {
    private static final String TAG = "ClearAllController";
    public static final boolean IS_SUPPORT_CLEAR_ALL_ON_BOTTOM =
            FeatureOption.SPRD_CLEAR_ALL_ON_BOTTOM_SUPPORT.get()
                    && (!GO_LOW_RAM_RECENTS_ENABLED || MeminfoController.IS_SUPPORT_SHOW_MEMINFO);
    private static final long ANIM_DURATION = 120;
    private static final float PROGRESS_END = 0.1f;

    public static final FloatProperty<ClearAllController> HIDE_PROGRESS =
            new FloatProperty<ClearAllController>("Clear all Progress") {

                @Override
                public Float get(ClearAllController controller) {
                    return controller.getProgress();
                }

                @Override
                public void setValue(ClearAllController controller, float progress) {
                    controller.setProgress(progress);
                }
            };

    private AnimatorSet mAmin;
    private boolean mHasTask;
    private ClearAllButton mClearAllButton;
    private float mProgress = 0f;
    private boolean mEnable = false;

    public ClearAllController(BaseDraggingActivity activity) {
        View rv = activity.getOverviewPanel();
        if (rv instanceof Callbacks) {
            mClearAllButton = (ClearAllButton) LayoutInflater.from(activity.getDragLayer().getContext())
                    .inflate(R.layout.clearallbutton_ext, activity.getDragLayer(), false);
            activity.getDragLayer().addView(mClearAllButton);
            ((Callbacks) rv).bindClearAllController(this);

            if (activity instanceof Launcher) {
                ((Launcher) activity).getStateManager().addStateListener(this);
            }
        }
    }

    @Override
    public void onStateTransitionStart(LauncherState toState) {
        if (!toState.overviewUi || toState.disableInteraction) {
            showOrHide(false);
        }
    }

    @Override
    public void onStateTransitionComplete(LauncherState finalState) {
        showOrHide(finalState.overviewUi && !finalState.disableInteraction);
    }

    public void onTaskStackUpdated(boolean hasTask) {
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "onTaskStackUpdated hastask: " + mHasTask + " --> " + hasTask);
        }
        mHasTask = hasTask;
        if (isNeedRefresh()) {
            showOrHide();
        }
    }

    public void setClearAllOnClickListener(View.OnClickListener listener) {
        mClearAllButton.setClearAllOnClickListener(listener);
    }

    private void showOrHide(boolean enable) {
        mEnable = enable;
        if (isNeedRefresh()) {
            showOrHide();
        }
    }

    private void showOrHide() {
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "showOrHide mEnable: " + mEnable);
        }
        if (mAmin != null) {
            mAmin.cancel();
        }
        mAmin = new AnimatorSet();

        ObjectAnimator clearall = ObjectAnimator.ofFloat(mClearAllButton,
                ClearAllButton.CONTENT_ALPHA, mEnable && mHasTask ? 1f : 0f);
        clearall.setDuration(ANIM_DURATION);
        clearall.setInterpolator(LINEAR);
        mAmin.play(clearall);
        mAmin.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAmin = null;
            }
        });
        mAmin.start();
    }

    private boolean isNeedRefresh() {
        if (mAmin != null) {
            return true;
        }
        boolean needShow = mEnable && mHasTask;
        return (needShow && Float.compare(mClearAllButton.getContentAlpha(), 1f) != 0)
                || (!needShow && (Float.compare(mClearAllButton.getContentAlpha(), 0f) != 0));
    }

    public void showImmediately() {
        showOrHide(true);
    }

    public void setProgress(float progress) {
        mProgress = progress;
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "setProgress progress: " + progress);
        }
        float alpha = (progress < PROGRESS_END) ?
                (PROGRESS_END - progress) / PROGRESS_END : 0f;
        mClearAllButton.setContentAlpha(alpha);
    }

    public float getProgress() {
        return mProgress;
    }

    public interface Callbacks {
        void bindClearAllController(ClearAllController controller);
    }

    @VisibleForTesting
    public AnimatorSet getAmin() {
        return mAmin;
    }

    @VisibleForTesting
    public ClearAllController(ClearAllButton button) {
        mClearAllButton = button;
    }
}

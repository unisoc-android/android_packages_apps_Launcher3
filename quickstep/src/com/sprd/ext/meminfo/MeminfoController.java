package com.sprd.ext.meminfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;

import androidx.annotation.VisibleForTesting;

import static com.android.launcher3.anim.Interpolators.LINEAR;

import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.R;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LogUtils;

public class MeminfoController implements LauncherStateManager.StateListener {
    private static final String TAG = "MeminfoController";
    public static final boolean IS_SUPPORT_SHOW_MEMINFO = FeatureOption.SPRD_SHOW_MEMINFO_SUPPORT.get();

    private static final long ANIM_DURATION = 120;
    private AnimatorSet mAmin;
    private MeminfoView mMeminfoView;

    public MeminfoController(BaseDraggingActivity activity) {
        mMeminfoView = activity.findViewById(R.id.recents_memoryinfo);
        if (activity instanceof Launcher) {
            ((Launcher) activity).getStateManager().addStateListener(this);
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

    public void showImmediately() {
        showOrHide(true);
    }

    private void showOrHide(boolean show) {
        LogUtils.d(TAG, "showOrHide show: " + show);
        if (mAmin != null) {
            mAmin.cancel();
        }
        mAmin = new AnimatorSet();
        ObjectAnimator meminfo = ObjectAnimator.ofFloat(mMeminfoView,
                MeminfoView.CONTENT_ALPHA, show ? 1f : 0f);
        meminfo.setDuration(ANIM_DURATION);
        meminfo.setInterpolator(LINEAR);
        mAmin.play(meminfo);
        mAmin.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAmin = null;
            }
        });
        mAmin.start();
    }

    @VisibleForTesting
    public AnimatorSet getAmin() {
        return mAmin;
    }
}

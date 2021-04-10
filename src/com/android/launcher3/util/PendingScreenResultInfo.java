package com.android.launcher3.util;

public class PendingScreenResultInfo {
    public final boolean animate;
    public final Runnable onComplete;
    public final int delay;
    public final boolean stripEmptyScreens;

    public PendingScreenResultInfo(final boolean animate, final Runnable onComplete,
                                 final int delay, final boolean stripEmptyScreens) {
        this.animate = animate;
        this.onComplete = onComplete;
        this.delay = delay;
        this.stripEmptyScreens = stripEmptyScreens;
    }
}

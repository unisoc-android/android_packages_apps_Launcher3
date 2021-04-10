package com.sprd.ext.lockicon;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.launcher3.R;
import com.android.systemui.shared.recents.model.Task;
import com.sprd.ext.FeatureOption;

public interface LockInfo {

    boolean isLocked();

    void setLocked(boolean isLocked);

    String getKey();

    void setKey(String str);

    void setLockIcon(Drawable drawable);

    default void initLockedStatus(Context context, Task task) {
        if (FeatureOption.SPRD_TASK_LOCK_SUPPORT.get()) {
            setKey(TaskLockStatus.makeTaskStringKey(context, task));
            setLocked(TaskLockStatus.isSavedLockedTask(context, getKey()));
            setLockIcon(getDrawable(context));
        }
    }

    default void changeLockState(Context context) {
        if (FeatureOption.SPRD_TASK_LOCK_SUPPORT.get() && TaskLockStatus.setLockState(context, getKey(), !isLocked())) {
            setLocked(!isLocked());
            setLockIcon(getDrawable(context));
        }
    }

    default Drawable getDrawable(Context context) {
        if (FeatureOption.SPRD_TASK_LOCK_SUPPORT.get()) {
            return isLocked() ? context.getDrawable(R.drawable.lock_icon_ext)
                    : context.getDrawable(R.drawable.unlock_icon_ext);
        }
        return null;
    }
}

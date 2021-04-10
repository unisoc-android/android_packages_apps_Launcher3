package com.android.quickstep;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.launcher3.BaseActivity;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.popup.SystemShortcut;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static android.widget.Toast.LENGTH_SHORT;

public class TaskSystemShortcut extends SystemShortcut {

    public static List<TaskSystemShortcut> getEnabledShortcuts(
            Context context, TaskActionController controller, Task task) {
        final ArrayList<TaskSystemShortcut> shortcuts = new ArrayList<>();
        final BaseDraggingActivity activity = BaseActivity.fromContext(context);
        TaskSystemShortcut menuOption = new TaskSystemShortcut.Pin();
        View.OnClickListener onClickListener =
                menuOption.getOnClickListener(activity, controller, task);
        if (onClickListener != null) {
            shortcuts.add(menuOption);
        }
        return shortcuts;
    }

    private TaskSystemShortcut(int iconResId, int labelResId) {
        super(iconResId, labelResId);
    }

    @Override
    public View.OnClickListener getOnClickListener(
            BaseDraggingActivity activity, ItemInfo itemInfo) {
        return null;
    }

    public View.OnClickListener getOnClickListener(
            BaseDraggingActivity activity, TaskActionController controller, Task task) {
        return null;
    }

    public static class Pin extends TaskSystemShortcut {

        private static final String TAG = Pin.class.getSimpleName();

        private Handler mHandler;

        public Pin() {
            super(R.drawable.ic_pin, R.string.recent_task_option_pin);
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, TaskActionController controller, Task task) {
            ISystemUiProxy sysUiProxy = RecentsModel.INSTANCE.get(activity).getSystemUiProxy();
            if (sysUiProxy == null) {
                return null;
            }
            if (!ActivityManagerWrapper.getInstance().isScreenPinningEnabled()) {
                return null;
            }
            if (ActivityManagerWrapper.getInstance().isLockToAppActive()) {
                // We shouldn't be able to pin while an app is locked.
                return null;
            }
            return view -> {
                Consumer<Boolean> resultCallback = success -> {
                    Log.d(TAG, "getOnClickListener success: " + success);
                    if (success) {
                        try {
                            sysUiProxy.startScreenPinning(task.key.id);
                        } catch (RemoteException e) {
                            Log.w(TAG, "Failed to start screen pinning: ", e);
                        }
                    } else {
                        String msg = "Failed to launch task:";
                        if (task != null) {
                            msg += " task=" + task.key.baseIntent + " userId=" + task.key.userId;
                        }
                        Log.w(TAG, msg);
                        Toast.makeText(activity, R.string.activity_not_available, LENGTH_SHORT).show();
                    }
                };
                controller.launchTask(task, resultCallback, mHandler);
                dismissTaskMenuView(activity);
            };
        }
    }
}

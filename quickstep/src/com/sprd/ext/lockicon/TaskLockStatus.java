package com.sprd.ext.lockicon;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.android.launcher3.Utilities;
import com.android.systemui.shared.recents.model.Task;
import com.sprd.ext.LogUtils;

public class TaskLockStatus {
    private static final String TAG = "TaskLockStatus";

    public static boolean isSavedLockedTask(Context context, String stringKey) {
        if (TextUtils.isEmpty(stringKey)) {
            return false;
        }
        SharedPreferences sharedPref = Utilities.getPrefs(context.getApplicationContext());
        boolean isLocked = sharedPref.contains(stringKey);
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "stringKey: " + stringKey + " = " + isLocked);
        }
        return isLocked;
    }

    public static boolean setLockState(Context context, String stringKey, boolean isLocked) {
        if (TextUtils.isEmpty(stringKey)) {
            return false;
        }
        SharedPreferences sharedPref = Utilities.getPrefs(context.getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();

        if (isLocked) {
            editor.putInt(stringKey, 0).apply();
        } else {
            editor.remove(stringKey).apply();
        }
        return true;
    }

    public static String makeTaskStringKey(Context context, Task task) {
        if (task == null || task.key == null || task.key.baseIntent == null ||
                task.key.baseIntent.getComponent() == null) {
            return null;
        }

        ActivityInfo info;
        ComponentName cn = task.key.baseIntent.getComponent();
        try {
            PackageManager pm = context.getPackageManager();
            info = pm.getActivityInfo(cn, 0);
        } catch (PackageManager.NameNotFoundException nnfe) {
            info = null;
        }
        String name;
        if (info != null && !TextUtils.isEmpty(info.taskAffinity)) {
            if (LogUtils.DEBUG_ALL) {
                LogUtils.d(TAG, "taskAffinity: " + info.taskAffinity);
            }
            name = info.taskAffinity;
        } else {
            name = cn.toString();
        }

        String key = "u: " + task.key.userId + ", " + name;
        if (LogUtils.DEBUG_ALL) {
            LogUtils.d(TAG, "StringKey, " + key);
        }
        return key;
    }
}

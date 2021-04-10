package com.sprd.ext.dynamicicon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.R;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.util.LooperExecutor;
import com.sprd.ext.BaseController;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.dynamicicon.calendar.GoogleCalendarIcon;
import com.sprd.ext.dynamicicon.calendar.OriginalCalendarIcon;
import com.sprd.ext.dynamicicon.clock.GoogleClockIcon;
import com.sprd.ext.dynamicicon.clock.OriginalClockIcon;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

public class DynamicIconController extends BaseController implements Preference.OnPreferenceClickListener {
    private static final String TAG = "DynamicIconController";

    private static final int OP_ADD = 1;
    private static final int OP_UPDATE = 2;
    private static final int OP_REMOVE = 3;

    private static final String GOOGLE = "google";
    public static final String SEPARATOR = ",";

    private CopyOnWriteArrayList<DynamicIconCallback> mCallbacks = new CopyOnWriteArrayList<>();
    private HashMap<String, String> mConfigedDynamicInfos = new HashMap<>();
    ArrayList<String> mCalendarPkgs = new ArrayList<>();
    ArrayList<String> mClockPkgs = new ArrayList<>();

    private LauncherAppMonitorCallback mMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onLauncherStart() {
            for (DynamicIconCallback callback : mCallbacks) {
                callback.onStart();
            }
        }

        @Override
        public void onLauncherResumed() {
            for (DynamicIconCallback callback : mCallbacks) {
                callback.registerReceiver();
            }
        }

        @Override
        public void onLauncherPaused() {
            for (DynamicIconCallback callback : mCallbacks) {
                callback.unRegisterReceiver();
            }
        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            updateDynamicCallbacksForPkg(OP_REMOVE, packageName, user);
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            updateDynamicCallbacksForPkg(OP_ADD, packageName, user);
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            updateDynamicCallbacksForPkg(OP_UPDATE, packageName, user);
        }

        @Override
        public void onHomeStyleChanged(String style) {
            for (DynamicIconCallback callback : mCallbacks) {
                callback.removeDynamicIconFromDb();
            }
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            w.println();
            w.println(prefix + TAG + ": Dynamic icons: " + mCallbacks);
            if (dumpAll) {
                for (int i = 0; i < mCallbacks.size(); i++) {
                    DynamicIconCallback callback = mCallbacks.get(i);
                    w.println(prefix + TAG + ": [" + callback.getPkgName() +
                            "] state is:" + callback.isOpened());
                }
            }
        }
    };

    public DynamicIconController(Context context, LauncherAppMonitor monitor) {
        super(context);

        if (FeatureOption.SPRD_DYNAMIC_ICON_CALENDAR_SUPPORT) {
            initDynamicInfos(DynamicIconSettings.PREF_KEY_ORIGINAL_CALENDAR,
                    context.getString(R.string.original_calendar_pkg), mCalendarPkgs);

            initDynamicInfos(DynamicIconSettings.PREF_KEY_GOOGLE_CALENDAR,
                    context.getString(R.string.google_calendar_pkg), mCalendarPkgs);
        }

        if (FeatureOption.SPRD_DYNAMIC_ICON_CLOCK_SUPPORT) {
            initDynamicInfos(DynamicIconSettings.PREF_KEY_ORIGINAL_CLOCK,
                    context.getString(R.string.original_clock_pkg), mClockPkgs);

            initDynamicInfos(DynamicIconSettings.PREF_KEY_GOOGLE_CLOCK,
                    context.getString(R.string.google_clock_pkg), mClockPkgs);
        }

        enableDynamicIconActivity();
        monitor.registerCallback(mMonitorCallback);

        if (LogUtils.DEBUG_DYNAMIC_ICON) {
            LogUtils.d(TAG, "Dynamic icons: " + mCallbacks);
        }
    }

    private void enableDynamicIconActivity() {
        final ComponentName cn = new ComponentName(mContext, DynamicIconSettings.class);
        mContext.getPackageManager().setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    void initDynamicInfos(String prefKey, String pkg, ArrayList<String> dynamicPkgs) {
        if (TextUtils.isEmpty(prefKey) || TextUtils.isEmpty(pkg)) {
            return;
        }
        dynamicPkgs.add(pkg);
        mConfigedDynamicInfos.put(prefKey, pkg);

        if (UtilitiesExt.isAppInstalled(mContext, pkg, Process.myUserHandle())) {
            DynamicIconCallback callback = createDynamicIcon(pkg);
            if (callback != null) {
                mCallbacks.add(callback);
            }
        }
    }

    private DynamicIconCallback createDynamicIcon(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return null;
        }

        DynamicIconCallback callback = null;

        if (FeatureOption.SPRD_DYNAMIC_ICON_CALENDAR_SUPPORT) {
            if (mCalendarPkgs.contains(pkg)) {
                if (pkg.contains(GOOGLE)) {
                    callback = new GoogleCalendarIcon(mContext, pkg);
                } else {
                    callback = new OriginalCalendarIcon(mContext, pkg);
                }
            }
        }

        if (callback == null && FeatureOption.SPRD_DYNAMIC_ICON_CLOCK_SUPPORT) {
            if (mClockPkgs.contains(pkg)) {
                if (pkg.contains(GOOGLE)) {
                    callback = new GoogleClockIcon(mContext, pkg);
                } else {
                    callback = new OriginalClockIcon(mContext, pkg);
                }
            }
        }

        return callback;
    }

    @VisibleForTesting
    void clearDynamicIcons() {
        mCalendarPkgs.clear();
        mClockPkgs.clear();
        mConfigedDynamicInfos.clear();
        mCallbacks.clear();
    }

    private void updateDynamicCallbacksForPkg(int op, String pkg, UserHandle user) {
        if (mConfigedDynamicInfos.containsValue(pkg)) {
            LooperExecutor workExecutor = new LooperExecutor(LauncherModel.getWorkerLooper());
            workExecutor.execute(() -> {
                switch (op) {
                    case OP_ADD:
                        addDynamicCallbacksForPkg(pkg, user);
                        break;
                    case OP_UPDATE:
                        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(mContext);
                        final List<LauncherActivityInfo> matches = launcherApps.getActivityList(pkg,
                                user);
                        if (matches.size() > 0) {
                            addDynamicCallbacksForPkg(pkg, user);
                        } else {
                            removeDynamicCallbacksForPkg(pkg, user);
                        }
                        break;
                    case OP_REMOVE:
                        removeDynamicCallbacksForPkg(pkg, user);
                        break;
                }
            });
        }
    }

    public boolean hasDynamicIcon() {
        return mCallbacks.size() > 0;
    }

    void addDynamicCallbacksForPkg(String pkg, UserHandle user) {
        if (!getInstalledDynamicPkgs().contains(pkg)) {
            DynamicIconCallback callback = createDynamicIcon(pkg);
            if (callback != null) {
                mCallbacks.add(callback);
            }

            if (LogUtils.DEBUG_DYNAMIC_ICON) {
                LogUtils.d(TAG, "Add " + pkg + ", mCallbacks = " + mCallbacks);
            }
        }
    }

    void removeDynamicCallbacksForPkg(String pkg, UserHandle user) {
        int index = -1;
        boolean found = false;
        for (int i = 0; i < mCallbacks.size(); i++) {
            DynamicIconCallback callback = mCallbacks.get(i);
            if (callback.getPkgName().equals(pkg)) {
                index = i;
                found = true;
                break;
            }
        }

        if (found) {
            DynamicIconCallback callback = mCallbacks.remove(index);
            callback.unRegisterReceiver();

            String prefKey = getPrefKeysByPackageName(pkg);
            DynamicIconUtils.removePrefKeyFromSharedPref(mContext, pkg, prefKey);

            if (LogUtils.DEBUG_DYNAMIC_ICON) {
                LogUtils.d(TAG, "Remove " + pkg + ", mCallbacks = " + mCallbacks);
            }
        }
    }

    ArrayList<String> getInstalledDynamicPkgs() {
        ArrayList<String> installedPkgs = new ArrayList<>();

        for (DynamicIconCallback callback : mCallbacks) {
            installedPkgs.add(callback.getPkgName());
        }
        return installedPkgs;
    }

    private String getPrefKeysByPackageName(String pkg) {
        for (Map.Entry<String, String> entry : mConfigedDynamicInfos.entrySet()) {
            if (entry.getValue().equals(pkg)) {
                return entry.getKey();
            }
        }
        return "";
    }

    String getPackageNameByPrefKey(String prefKey) {
        return mConfigedDynamicInfos.get(prefKey);
    }

    void onSettingChanged(String pkg, boolean dynamic) {
        if (!TextUtils.isEmpty(pkg)) {
            for (DynamicIconCallback callback : mCallbacks) {
                if (pkg.equals(callback.getPkgName())) {
                    callback.onStateChanged(dynamic);
                    break;
                }
            }
        }
    }

    void updateIcons(String pkg) {
        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState != null) {
            LauncherModel model = appState.getModel();
            UserManagerCompat userManager = UserManagerCompat.getInstance(mContext);
            final List<UserHandle> profiles = userManager.getUserProfiles();
            for (UserHandle user : profiles) {
                model.onPackageChanged(pkg, user);
                List<ShortcutInfo> queryForPinnedShortcuts = DeepShortcutManager.getInstance(mContext)
                        .queryForPinnedShortcuts(pkg, user);
                if (!queryForPinnedShortcuts.isEmpty()) {
                    model.updatePinnedShortcuts(pkg, queryForPinnedShortcuts, user);
                }
            }
        }
    }

    public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
        String pkg = info.getApplicationInfo().packageName;

        for (DynamicIconCallback callback : mCallbacks) {
            if (callback.getPkgName().equals(pkg)) {
                Drawable drawable = callback.getIcon(info, iconDpi, flattenDrawable);
                if (drawable == null) {
                    if (LogUtils.DEBUG_DYNAMIC_ICON) {
                        LogUtils.d(TAG, "get dynamic icon is null for:" + pkg);
                    }
                }
                return drawable;
            }
        }
        return null;
    }

    public String getSystemStateForPackage(String systemState, String pkg) {
        for (DynamicIconCallback callback : mCallbacks) {
            if (callback.getPkgName().equals(pkg)) {
                StringBuilder builder = new StringBuilder();
                builder.append(systemState);

                String state = callback.getSystemState();
                if (!TextUtils.isEmpty(state)) {
                    builder.append(SEPARATOR).append(state);
                }

                return builder.toString();
            }
        }
        return systemState;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mContext != null) {
            Intent it = new Intent(mContext.getApplicationContext(), DynamicIconSettings.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(it);
            return true;
        }
        return false;
    }
}

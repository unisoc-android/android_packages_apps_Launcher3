package com.sprd.ext.multimode;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.os.UserHandle;
import android.util.Pair;

import com.android.launcher3.AppInfo;
import com.android.launcher3.InstallShortcutReceiver;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.sprd.ext.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class VerifyIdleAppTask implements Runnable {
    private static final String TAG = "VerifyIdleAppTask";

    private final Context mContext;
    private final Collection<AppInfo> mApps;
    private final String mPackageNames;
    private final UserHandle mUser;
    private final boolean mIsAddPackage;
    private boolean mIgnoreLoaded;

    VerifyIdleAppTask(Context context, Collection<AppInfo> apps, String packageNames, UserHandle user, boolean isAdd) {
        mContext = context;
        mApps = apps;
        mPackageNames = packageNames;
        mUser = user;
        mIsAddPackage = isAdd;
    }

    @Override
    public void run() {
        final Map<ComponentKey, Object> map = new HashMap<>();
        if (mApps != null && mApps.size() > 0) {
            // All apps loading, we ignore loaded.
            mIgnoreLoaded = true;
            for (AppInfo app : mApps) {
                map.put(app.toComponentKey(), app);
            }
        } else if (mPackageNames != null && mUser != null) {
            // App add or update, should not ignore loaded.
            mIgnoreLoaded = false;
            final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(mContext);
            if (mIsAddPackage || launcherApps.isPackageEnabledForProfile(mPackageNames, mUser)) {
                final List<LauncherActivityInfo> infos = launcherApps.getActivityList(mPackageNames, mUser);
                for (LauncherActivityInfo info : infos) {
                    map.put(new ComponentKey(info.getComponentName(), info.getUser()), info);
                }
            }
        }

        verifyAllApps(mContext, map, mIsAddPackage);
    }

    List<Pair<ItemInfo, Object>> verifyAllApps(Context context, Map<ComponentKey, Object> map, boolean animated) {
        List<Pair<ItemInfo, Object>> newItems = new ArrayList<>();
        synchronized (LauncherModel.getBgDataModel()) {
            for (Map.Entry<ComponentKey, Object> entry : map.entrySet()) {
                ComponentKey componentKey = entry.getKey();
                HashSet<ComponentName> components = new HashSet<>(1);
                components.add(componentKey.componentName);
                ItemInfoMatcher matcher = ItemInfoMatcher.ofComponents(components, componentKey.user);
                if (matcher.filterItemInfos(LauncherModel.getBgDataModel().workspaceItems).isEmpty()) {
                    Object obj = entry.getValue();
                    if (obj instanceof AppInfo) {
                        verifyShortcutHighRes(context, (AppInfo) obj);
                        newItems.add(Pair.create((AppInfo) obj, null));
                    } else if (obj instanceof LauncherActivityInfo) {
                        LauncherActivityInfo info = (LauncherActivityInfo) obj;
                        newItems.add(Pair.create(InstallShortcutReceiver.fromActivityInfo(info, context), null));
                    }
                    if (LogUtils.DEBUG_ALL) {
                        LogUtils.d(TAG, "will bind " + componentKey.componentName + " to workspace.");
                    }
                }
            }
        }

        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState != null && newItems.size() > 0) {
            appState.getModel().addAndBindAddedWorkspaceItems(newItems, animated, mIgnoreLoaded);
        }
        return newItems;
    }

    private static void verifyShortcutHighRes(Context context, AppInfo appInfo) {
        if (appInfo != null) {
            if (appInfo.usingLowResIcon()) {
                LauncherAppState.getInstance(context).getIconCache().getTitleAndIcon(appInfo, false);
            }
        }
    }
}

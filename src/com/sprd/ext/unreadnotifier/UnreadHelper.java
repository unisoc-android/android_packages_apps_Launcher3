package com.sprd.ext.unreadnotifier;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.AppInfo;
import com.android.launcher3.CellLayout;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Workspace;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.util.ComponentKey;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.multimode.MultiModeController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class is a util class, implemented to do the following things,:
 * Receive unread broadcast sent by application, update shortcuts and folders in
 * workspace, hot seat and update application icons in app customize paged view.
 */
public class UnreadHelper extends BroadcastReceiver {
    private static final String TAG = "UnreadHelper";

    private static final String ACTION_UNREAD_CHANGED = "com.sprd.action.UNREAD_CHANGED";
    private static final String EXTRA_UNREAD_COMPONENT = "com.sprd.intent.extra.UNREAD_COMPONENT";
    private static final String EXTRA_UNREAD_NUMBER = "com.sprd.intent.extra.UNREAD_NUMBER";

    private static final String PREFS_FILE_NAME = TAG + "_Pref";

    private static final ArrayList<UnreadSupportShortcut> UNREAD_SUPPORT_SHORTCUTS =
            new ArrayList<>();
    private static int sUnreadSupportShortcutsNum = 0;
    private static final Object LOG_LOCK = new Object();
    private static final Handler mUiHandler = new Handler();

    private static final int UNREAD_TYPE_INTERNAL = 0;
    private static final int UNREAD_TYPE_EXTERNAL = 1;
    private static final int INVALID_NUM = -1;

    private boolean mRegistered = false;

    private Context mContext;
    private SharedPreferences mSharePrefs;

    UnreadHelper(Context context) {
        mContext = context;
        mSharePrefs = mContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    }

    void registerUnreadReceiver() {
        if (!mRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_UNREAD_CHANGED);
            if (LogUtils.DEBUG_UNREAD) LogUtils.d(TAG, "registerUnreadReceiver: this = " + this);
            mContext.registerReceiver(this, filter);
            mRegistered = true;
        }
    }

    void unRegisterUnreadReceiver() {
        if (mRegistered) {
            mRegistered = false;
            if (LogUtils.DEBUG_UNREAD) LogUtils.d(TAG, "unRegisterUnreadReceiver: this = " + this);
            mContext.unregisterReceiver(this);
        }
    }

    void initializeUnreadInfo(UnreadInfoManager unreadInfoManager) {
        unreadInfoManager.createItemIfNeeded();
        unreadInfoManager.initAppsAndPermissionList();
        loadInitialUnreadInfo(unreadInfoManager);
        unreadInfoManager.initUnreadInfo();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (ACTION_UNREAD_CHANGED.equals(action)) {
            final ComponentName componentName = ComponentName.unflattenFromString(
                    Objects.requireNonNull(intent.getStringExtra(EXTRA_UNREAD_COMPONENT)));
            final int unreadNum = intent.getIntExtra(EXTRA_UNREAD_NUMBER, INVALID_NUM);

            updateComponentUnreadInfo(unreadNum, componentName, Process.myUserHandle());
        }
    }

    void updateComponentUnreadInfo(int unreadNum, ComponentName componentName, UserHandle userHandle) {
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateComponentUnreadInfo : componentName = " + componentName
                    + ", unreadNum = " + unreadNum);
        }

        if (componentName != null && unreadNum != INVALID_NUM && userHandle != null) {
            saveAndUpdateUI(componentName, unreadNum, userHandle);
        }
    }

    private void saveAndUpdateUI(final ComponentName component, final int unreadNum, UserHandle userHandle) {
        ComponentKey componentKey = new ComponentKey(component, userHandle);
        final String sharedPrefKey = UtilitiesExt.componentKeyToString(mContext, componentKey);
        final int index = supportUnreadFeature(component, userHandle);
        boolean needUpdate = false;
        if (index != INVALID_NUM) {
            if (UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum != unreadNum) {
                saveUnreadNum(sharedPrefKey, unreadNum);
                UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum = unreadNum;
                if (LogUtils.DEBUG_UNREAD) {
                    LogUtils.d(TAG, "saveAndUpdateUI,update SupportList, key:" + sharedPrefKey + " success.");
                }
                needUpdate = true;
            }
        } else {
            // add new info
            if (unreadNum > 0) {
                saveUnreadNum(sharedPrefKey, unreadNum);
                UnreadSupportShortcut usShortcut = new UnreadSupportShortcut(
                        component.flattenToShortString(), userHandle, UNREAD_TYPE_EXTERNAL);
                usShortcut.mUnreadNum = unreadNum;
                UNREAD_SUPPORT_SHORTCUTS.add(usShortcut);
                sUnreadSupportShortcutsNum = UNREAD_SUPPORT_SHORTCUTS.size();
                if (LogUtils.DEBUG_UNREAD) {
                    LogUtils.d(TAG, "saveAndUpdateUI, add To SupportList, key:" + sharedPrefKey + " success."
                            + getUnreadSupportShortcutInfo());
                }
                needUpdate = true;
            }
        }

        if (needUpdate) {
            UnreadKeyData unreadKeyData = new UnreadKeyData(new ComponentKey(component, userHandle), unreadNum);
            onUnreadInfoChanged(unreadKeyData);
        }
    }

    private int readUnreadNum(final String key) {
        return mSharePrefs.getInt(key, INVALID_NUM);
    }

    private void saveUnreadNum(final String key, final int unReadNum) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.putInt(key, unReadNum).apply();
    }

    private void deleteUnreadNum(final String key) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.remove(key).apply();
    }

    /**
     * Get unread support shortcut information, since the information are stored
     * in an array list, we may query it and modify it at the same time, a lock
     * is needed.
     *
     * @return SupportShortString
     */
    private static String getUnreadSupportShortcutInfo() {
        String info = " Unread support shortcuts are ";
        ArrayList<UnreadSupportShortcut> logList = new ArrayList<>(UNREAD_SUPPORT_SHORTCUTS);
        synchronized (LOG_LOCK) {
            info += logList.toString();
        }
        return info;
    }

    /**
     * Whether the given component support unread feature.
     *
     * @param component component
     * @return array index, find fail return INVALID_NUM
     */
    static int supportUnreadFeature(ComponentName component, UserHandle userHandle) {
        if (component == null) {
            return INVALID_NUM;
        }

        final int size = UNREAD_SUPPORT_SHORTCUTS.size();
        for (int i = 0; i < size; i++) {
            UnreadSupportShortcut supportShortcut = UNREAD_SUPPORT_SHORTCUTS.get(i);
            if (supportShortcut.mComponent.equals(component) && supportShortcut.mUserHandle.equals(userHandle)) {
                return i;
            }
        }

        return INVALID_NUM;
    }

    private void loadInitialUnreadInfo(UnreadInfoManager unreadInfoManager) {
        long start = System.currentTimeMillis();
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "loadUnreadSupportShortcuts begin: start = " + start);
        }

        UNREAD_SUPPORT_SHORTCUTS.clear();
        for (String sharedPrefKey : mSharePrefs.getAll().keySet()) {
            boolean needCreatePackageUserKey = false;
            ComponentKey componentKey = UtilitiesExt.stringToComponentKey(mContext, sharedPrefKey);
            ComponentName componentName = componentKey.componentName;
            UserHandle userHandle = componentKey.user;
            String key = componentName.flattenToShortString();
            int loadNum = 0;
            if (!unreadInfoManager.isDeniedPermissionItem(key)) {
                loadNum = readUnreadNum(sharedPrefKey);
                needCreatePackageUserKey = loadNum > 0;
            }

            if (needCreatePackageUserKey) {
                UnreadSupportShortcut usShortcut = new UnreadSupportShortcut(
                        key, userHandle, UNREAD_TYPE_INTERNAL);
                usShortcut.mUnreadNum = loadNum;
                if (!UNREAD_SUPPORT_SHORTCUTS.contains(usShortcut)) {
                    UNREAD_SUPPORT_SHORTCUTS.add(usShortcut);
                }
            } else {
                deleteUnreadNum(sharedPrefKey);
            }
        }
        sUnreadSupportShortcutsNum = UNREAD_SUPPORT_SHORTCUTS.size();

        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "loadUnreadSupportShortcuts end: time used = "
                    + (System.currentTimeMillis() - start) + ",sUnreadSupportShortcutsNum = "
                    + sUnreadSupportShortcutsNum + getUnreadSupportShortcutInfo());
        }
    }

    // update
    void onUnreadInfoChanged(final UnreadKeyData info) {
        Launcher launcher = LauncherAppMonitor.getInstanceNoCreate().getLauncher();
        if (launcher == null || info == null) {
            return;
        }
        mUiHandler.post(() -> {
            onWorkspaceUnreadInfoAppUpdated(info);
            if (!MultiModeController.isSingleLayerMode()) {
                onAllAppsUnreadInfoAppUpdated(info);
            }
        });
    }

    private void onWorkspaceUnreadInfoAppUpdated(UnreadKeyData unreadKeyData) {
        Launcher launcher = LauncherAppMonitor.getInstanceNoCreate().getLauncher();
        if (launcher != null) {
            Workspace workspace = launcher.getWorkspace();
            for (final CellLayout layoutParent : workspace.getWorkspaceAndHotseatCellLayouts()) {
                final ViewGroup layout = layoutParent.getShortcutsAndWidgets();
                final int itemCount = layout.getChildCount();
                for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
                    View item = layout.getChildAt(itemIdx);
                    ItemInfo info = (ItemInfo) item.getTag();
                    if (info instanceof FolderInfo) {
                        if (updateFolderUnreadNum((FolderInfo) info, unreadKeyData)) {
                            item.invalidate();
                        }
                    } else if (info instanceof WorkspaceItemInfo) {
                        if (!isUnreadItemType(info.itemType)) continue;
                        final WorkspaceItemInfo workspaceItemInfo = (WorkspaceItemInfo) info;
                        final Intent intent = workspaceItemInfo.getIntent();
                        final ComponentName component = intent.getComponent();
                        if (component != null
                                && component.equals(unreadKeyData.componentKey.componentName)
                                && info.user.equals(unreadKeyData.componentKey.user)) {
                            if (workspaceItemInfo.unreadNum != unreadKeyData.unreadNum) {
                                workspaceItemInfo.unreadNum = unreadKeyData.unreadNum;
                                item.invalidate();
                            }
                        }
                    }
                }
            }
        }
    }

    private void onAllAppsUnreadInfoAppUpdated(UnreadKeyData unreadKeyData) {
        Launcher launcher = LauncherAppMonitor.getInstanceNoCreate().getLauncher();
        if (launcher != null) {
            AllAppsContainerView allAppsContainerView = launcher.getAppsView();
            if (allAppsContainerView != null) {
                List<AppInfo> allApps = allAppsContainerView.getApps().getApps();
                List<AppInfo> updateApps = new ArrayList<>();
                for (AppInfo appInfo : allApps) {
                    ComponentName name = appInfo.intent.getComponent();
                    if (name != null
                            && name.equals(unreadKeyData.componentKey.componentName)
                            && appInfo.user.equals(unreadKeyData.componentKey.user)) {
                        if (appInfo.unreadNum != unreadKeyData.unreadNum) {
                            appInfo.unreadNum = unreadKeyData.unreadNum;
                            updateApps.add(appInfo);
                        }
                    }
                }
                allAppsContainerView.getAppsStore().addOrUpdateApps(updateApps);
            }
        }
    }

    boolean updateFolderUnreadNum(FolderInfo folderInfo, UnreadKeyData unreadKeyData) {
        if (folderInfo == null) {
            return false;
        }
        int unreadNumTotal = 0;
        ComponentName componentName;
        for (WorkspaceItemInfo wsi : folderInfo.contents) {
            if (!isUnreadItemType(wsi.itemType)) continue;
            componentName = wsi.getTargetComponent();
            if (componentName != null
                    && componentName.equals(unreadKeyData.componentKey.componentName)
                    && wsi.user.equals(unreadKeyData.componentKey.user)) {
                wsi.unreadNum = unreadKeyData.unreadNum;
            }
            if (wsi.unreadNum > 0) {
                unreadNumTotal += wsi.unreadNum;
            }
        }

        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateFolderUnreadNum, end: unreadNumTotal = " + unreadNumTotal);
        }

        return setFolderUnreadNum(folderInfo, unreadNumTotal);
    }

    private boolean setFolderUnreadNum(FolderInfo folderInfo, int unreadNum) {
        if (folderInfo == null) {
            return false;
        }
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "setFolderUnreadNum: unreadNum = " + unreadNum + ", info = " + folderInfo);
        }

        if (unreadNum <= 0) {
            unreadNum = 0;
        }

        if (unreadNum != folderInfo.unreadNum) {
            folderInfo.unreadNum = unreadNum;
            return true;
        }
        return false;
    }


    static synchronized int getUnreadNumberAt(int index) {
        if (index < 0 || index >= sUnreadSupportShortcutsNum) {
            return 0;
        }
        return UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum;
    }

    private int getUnreadNumberOfComponent(ComponentName component, UserHandle userHandle) {
        final int index = supportUnreadFeature(component, userHandle);
        return getUnreadNumberAt(index);
    }

    // Full refresh shortcut and folder unreadInfo
    void onWorkspaceUnreadInfoAllUpdated() {
        Launcher launcher = LauncherAppMonitor.getInstanceNoCreate().getLauncher();
        if (launcher != null) {
            Workspace workspace = launcher.getWorkspace();
            for (final CellLayout layoutParent : workspace.getWorkspaceAndHotseatCellLayouts()) {
                final ViewGroup layout = layoutParent.getShortcutsAndWidgets();
                final int itemCount = layout.getChildCount();
                for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
                    View item = layout.getChildAt(itemIdx);
                    ItemInfo info = (ItemInfo) item.getTag();
                    if (info instanceof FolderInfo) {
                        if (updateFolderUnreadNum((FolderInfo) info)) {
                            item.invalidate();
                        }
                    } else if (info instanceof WorkspaceItemInfo) {
                        if (!isUnreadItemType(info.itemType)) continue;
                        final WorkspaceItemInfo shortcutInfo = (WorkspaceItemInfo) info;
                        final Intent intent = shortcutInfo.getIntent();
                        final ComponentName component = intent.getComponent();
                        final int num = getUnreadNumberOfComponent(component, shortcutInfo.user);
                        if (info.unreadNum != num) {
                            info.unreadNum = num;
                            item.invalidate();
                        }
                    }
                }
            }
        }
    }

    private boolean updateFolderUnreadNum(FolderInfo folderInfo) {
        if (folderInfo == null) {
            return false;
        }
        int unreadNumTotal = 0;
        ComponentName componentName;
        int unreadNum;
        for (WorkspaceItemInfo si : folderInfo.contents) {
            if (!isUnreadItemType(si.itemType)) continue;
            componentName = si.getIntent().getComponent();
            unreadNum = getUnreadNumberOfComponent(componentName, si.user);
            if (unreadNum > 0) {
                si.unreadNum = unreadNum;
                unreadNumTotal += unreadNum;
            }
        }

        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateFolderUnreadNum end: unreadNumTotal = " + unreadNumTotal);
        }
        return setFolderUnreadNum(folderInfo, unreadNumTotal);
    }

    // Full refresh all apps unreadInfo
    void onAllAppsUnreadInfoAllUpdated(ArrayList<AppInfo> apps) {
        Launcher launcher = LauncherAppMonitor.getInstanceNoCreate().getLauncher();
        if (launcher != null) {
            AllAppsContainerView allAppsContainerView = launcher.getAppsView();
            if (allAppsContainerView != null) {
                if (LogUtils.DEBUG_UNREAD) {
                    LogUtils.d(TAG, "onAllAppsUnreadInfoAllUpdated: apps.size: " + apps.size());
                }
                List<AppInfo> updateApps = new ArrayList<>();
                for (AppInfo appInfo : apps) {
                    ComponentName name = appInfo.getTargetComponent();
                    int num = getUnreadNumberOfComponent(name, appInfo.user);
                    if (appInfo.unreadNum != num) {
                        appInfo.unreadNum = num;
                        updateApps.add(appInfo);
                    }
                }
                allAppsContainerView.getAppsStore().addOrUpdateApps(updateApps);
            }
        }
    }

    void destoryUnreadInfo(UnreadInfoManager unreadInfoManager) {
        unreadInfoManager.unregisterItemContentObservers();
    }

    static boolean isUnreadItemType(int itemType) {
        boolean ret = false;

        switch (itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                ret = true;
                break;
            default:
                break;
        }
        return ret;
    }

    static class UnreadSupportShortcut {
        ComponentName mComponent;
        String mKey;
        int mShortcutType;
        int mUnreadNum;
        UserHandle mUserHandle;

        UnreadSupportShortcut(String keyString, UserHandle userHandle, int type) {
            mComponent = ComponentName.unflattenFromString(keyString);
            mKey = keyString;
            mShortcutType = type;
            mUnreadNum = 0;
            mUserHandle = userHandle;
        }

        @Override
        public String toString() {
            return "{UnreadSupportShortcut[" + mComponent + "], key = " + mKey + ",type = "
                    + mShortcutType + ",unreadNum = " + mUnreadNum + "}";
        }
    }
}

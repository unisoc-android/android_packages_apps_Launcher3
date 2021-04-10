package com.sprd.ext.unreadnotifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.android.launcher3.AppInfo;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.util.ComponentKey;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.multimode.MultiModeController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

import androidx.preference.Preference;

public class UnreadInfoController extends BaseController implements Preference.OnPreferenceClickListener {
    private final UnreadHelper mUnreadHelper;
    private final UnreadInfoManager mUnreadInfoManager;

    public UnreadInfoController(Context context, LauncherAppMonitor monitor) {
        super(context);

        mUnreadHelper = new UnreadHelper(mContext);
        mUnreadInfoManager = new UnreadInfoManager(mContext);

        monitor.registerCallback(mUnreadMonitorCallback);

        enableUnreadSettingsActivity();
    }

    private void enableUnreadSettingsActivity() {
        PackageManager pm = mContext.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(mContext, UnreadSettingsActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private LauncherAppMonitorCallback mUnreadMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onLauncherCreated() {
            // Register unread change broadcast.
            mUnreadHelper.registerUnreadReceiver();
            mUnreadHelper.initializeUnreadInfo(mUnreadInfoManager);
        }

        @Override
        public void onLauncherDestroy() {
            mUnreadHelper.unRegisterUnreadReceiver();
            mUnreadHelper.destoryUnreadInfo(mUnreadInfoManager);
        }

        @Override
        public void onLauncherRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            mUnreadInfoManager.handleRequestPermissionResult(requestCode,
                    permissions, grantResults);
        }

        @Override
        public void onBindingWorkspaceFinish() {
            mUnreadHelper.onWorkspaceUnreadInfoAllUpdated();
        }

        @Override
        public void onBindingAllAppFinish(ArrayList<AppInfo> apps) {
            if (MultiModeController.isSingleLayerMode()) return;
            mUnreadHelper.onAllAppsUnreadInfoAllUpdated(apps);
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            super.dump( prefix, fd, w, dumpAll );
        }
    };

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mContext != null) {
            Intent it = new Intent(mContext.getApplicationContext(), UnreadSettingsActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(it);
            return true;
        }
        return false;
    }

    public void updateFolderIconIfNeeded(FolderIcon folderIcon, WorkspaceItemInfo item) {
        if (UnreadHelper.isUnreadItemType(item.itemType)) {
            mUnreadHelper.updateFolderUnreadNum((FolderInfo) folderIcon.getTag(),
                    new UnreadKeyData(new ComponentKey(item.getTargetComponent(), item.user), item.unreadNum));
        }
    }

    UnreadHelper getUnreadHelper() {
        return mUnreadHelper;
    }

    public UnreadInfoManager getUnreadInfoManager() {
        return mUnreadInfoManager;
    }
}

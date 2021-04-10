package com.sprd.ext;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.UserHandle;

import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by SPRD on 2017/7/6.
 */

public class LauncherAppMonitorCallback {
    //Launcher activity Callbacks
    public void onLauncherPreCreate(Launcher launcher) { }

    public void onLauncherCreated() { }

    public void onLauncherPreResume() { }

    public void onLauncherResumed() { }

    public void onLauncherStart() { }

    public void onLauncherStop() { }

    public void onLauncherPrePaused() { }

    public void onLauncherPaused() { }

    public void onLauncherDestroy() { }

    public void onLauncherRequestPermissionsResult(int requestCode, String[] permissions,
                                                   int[] grantResults) { }

    public void onLauncherFocusChanged(boolean hasFocus) { }


    public void onHomeIntent() { }

    public void onBindingWorkspaceFinish () { }

    public void onBindingAllAppFinish (ArrayList<AppInfo> apps) { }

    //Launcher app Callbacks
    public void onAppCreated(Context context) { }

    public void onReceive(Intent intent) { }

    public void onUIConfigChanged() { }

    public void onThemeChanged() { }

    public void onAppSharedPreferenceChanged(String key) { }

    public void onPackageRemoved(String packageName, UserHandle user) { }

    public void onPackageAdded(String packageName, UserHandle user) { }

    public void onPackageChanged(String packageName, UserHandle user) { }

    public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) { }

    public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) { }

    public void onPackagesSuspended(String[] packageNames, UserHandle user) { }

    public void onPackagesUnsuspended(String[] packageNames, UserHandle user) { }

    public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) { }

    public void onAllAppsListUpdated(List<AppInfo> apps) { }

    public void onLauncherLocaleChanged() { }

    public void onLauncherOrientationChanged () { }

    public void onLauncherScreensizeChanged() { }

    public void onLoadAllAppsEnd(ArrayList<AppInfo> apps) { }

    public void onHomeStyleChanged(String style) { }

    //Launcher database callbacks
    public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) { }
}

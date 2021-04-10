package com.sprd.ext;

import static android.content.pm.ActivityInfo.CONFIG_LOCALE;
import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.launcher3.AppInfo;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.util.MainThreadInitializedObject;
import com.sprd.ext.customizeappsort.CustomizeAppSortController;
import com.sprd.ext.cyclescroll.CycleScrollController;
import com.sprd.ext.dynamicicon.DynamicIconController;
import com.sprd.ext.dynamicicon.DynamicIconUtils;
import com.sprd.ext.folder.FolderIconController;
import com.sprd.ext.gestures.GesturesController;
import com.sprd.ext.grid.DesktopGridController;
import com.sprd.ext.grid.HotseatController;
import com.sprd.ext.icon.IconLabelController;
import com.sprd.ext.multimode.MultiModeController;
import com.sprd.ext.navigationbar.NavigationBarController;
import com.sprd.ext.notificationdots.NotifyDotsNumController;
import com.sprd.ext.resolution.SRController;
import com.sprd.ext.unreadnotifier.UnreadInfoController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by SPRD on 2017/6/21.
 */

public class LauncherAppMonitor implements SharedPreferences.OnSharedPreferenceChangeListener,
        LauncherAppsCompat.OnAppsChangedCallbackCompat {
    private static final String TAG = "LauncherAppMonitor";

    // We do not need any synchronization for this variable as its only written on UI thread.
    public static final MainThreadInitializedObject<LauncherAppMonitor> INSTANCE =
            new MainThreadInitializedObject<>(LauncherAppMonitor::new);

    private final ArrayList<WeakReference<LauncherAppMonitorCallback>> mCallbacks = new ArrayList<>();

    private Launcher mLauncher;
    private UnreadInfoController mUnreadInfoController = null;
    private FolderIconController mFolderIconController = null;
    private MultiModeController mMultiModeController = null;
    private GesturesController mGesturesController = null;
    private CycleScrollController mCycleScrollController = null;
    private NavigationBarController mNavigationBarController = null;
    private DesktopGridController mDesktopGridController = null;
    private DynamicIconController mDynamicIconController = null;
    private CustomizeAppSortController mCustomizeAppSortController = null;
    private HotseatController mHotseatController = null;
    private SRController mSRController = null;
    private NotifyDotsNumController mNotifyDotsNumController = null;
    private IconLabelController mIconLabelController = null;

    public static LauncherAppMonitor getInstance(final Context context) {
        return INSTANCE.get(context.getApplicationContext());
    }

    public static LauncherAppMonitor getInstanceNoCreate() {
        return INSTANCE.getNoCreate();
    }

    //return null while launcher activity isn't running
    public Launcher getLauncher() {
        return mLauncher;
    }

    /**
     * Remove the given observer's callback.
     *
     * @param callback The callback to remove
     */
    public void unregisterCallback(LauncherAppMonitorCallback callback) {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            if (mCallbacks.get(i).get() == callback) {
                synchronized (mCallbacks) {
                    mCallbacks.remove(i);
                    if (LogUtils.DEBUG_LOADER) {
                        UtilitiesExt.DEBUG_PRINT_FUNCTIONNAME(callback);
                    }
                }
            }
        }
    }

    /**
     * Register to receive notifications about general Launcher app information
     * @param callback The callback to register
     */
    public void registerCallback(LauncherAppMonitorCallback callback) {
        // Prevent adding duplicate callbacks
        unregisterCallback(callback);
        synchronized (mCallbacks) {
            mCallbacks.add(new WeakReference<>(callback));
            if (LogUtils.DEBUG_LOADER) {
                UtilitiesExt.DEBUG_PRINT_FUNCTIONNAME(callback);
            }
        }
    }

    private LauncherAppMonitor(Context context) {
        if (LogUtils.DEBUG_LOADER) {
            UtilitiesExt.DEBUG_PRINT_FUNCTIONNAME(this);
        }
        LauncherAppsCompat.getInstance(context).addOnAppsChangedCallback(this);
        Utilities.getPrefs(context).registerOnSharedPreferenceChangeListener(this);

        if (FeatureOption.SPRD_MULTI_MODE_SUPPORT.get()) {
            mMultiModeController = new MultiModeController(context, this);
        }

        if (FeatureOption.SPRD_DESKTOP_GRID_SUPPORT.get()) {
            mDesktopGridController = new DesktopGridController(context, this);
        }

        if (FeatureOption.SPRD_HOTSEAT_ICON_ADAPTIVE_LAYOUT.get()) {
            mHotseatController = new HotseatController(context, this);
        }

        if(FeatureOption.SPRD_BADGE_SUPPORT.get()) {
            mUnreadInfoController = new UnreadInfoController(context, this);
        }

        if (DynamicIconUtils.anyDynamicIconSupport()) {
            mDynamicIconController = new DynamicIconController(context, this);
        }

        if(FeatureOption.SPRD_FOLDER_ICON_MODE_SUPPORT.get()) {
            mFolderIconController = new FolderIconController(context, this);
        }

        if (FeatureOption.SPRD_GESTURE_SUPPORT.get()) {
            mGesturesController = new GesturesController(context, this);
        }

        if (FeatureOption.SPRD_CYCLE_SCROLL_SUPPORT.get()) {
            mCycleScrollController = new CycleScrollController(context, this);
        }

        if (FeatureOption.SPRD_ALLAPP_CUSTOMIZE_SUPPORT.get()) {
            mCustomizeAppSortController = new CustomizeAppSortController(context, this);
        }

        if (NavigationBarController.hasNavigationBar(context)) {
            LogUtils.d(TAG, "hasNavigationBar is true");
            mNavigationBarController = new NavigationBarController(context, this);
        }

        if (SRController.isSupportResolutionSwitch()) {
            mSRController = new SRController(context, this);
        }

        if (FeatureOption.SPRD_NOTIFICATION_DOT_COUNT.get()) {
            mNotifyDotsNumController = new NotifyDotsNumController(context, this);
        }

        if (FeatureOption.SPRD_ICON_LABEL_LINE_SUPPORT.get()) {
            mIconLabelController = new IconLabelController(context);
        }
    }

    private static void DEBUG_PRINT_FUNCTIONNAME() {
        UtilitiesExt.DEBUG_PRINT_FUNCTIONNAME(UtilitiesExt.BASE_STACK_DEPTH, null);
    }

    private static void DEBUG_PRINT_FUNCTIONNAME(String msg) {
        UtilitiesExt.DEBUG_PRINT_FUNCTIONNAME(UtilitiesExt.BASE_STACK_DEPTH, msg);
    }

    public void onLauncherPreCreate(Launcher launcher) {
        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME("launcher:" + launcher);
        }
        if (mLauncher != null) {
            onLauncherDestroy(mLauncher);
        }
        mLauncher = launcher;

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPreCreate(launcher);
            }
        }
    }

    public void onLauncherCreated() {
        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherCreated();
            }
        }
    }

    public void onLauncherPreResume() {
        if (LogUtils.DEBUG_ALL) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPreResume();
            }
        }
    }

    public void onLauncherResumed() {
        if (LogUtils.DEBUG_ALL) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherResumed();
            }
        }
    }

    public void onLauncherPrePause() {
        if (LogUtils.DEBUG_ALL) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPrePaused();
            }
        }
    }

    public void onLauncherPaused() {
        if (LogUtils.DEBUG_ALL) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherPaused();
            }
        }
    }

    public void onLauncherStart() {
        if (LogUtils.DEBUG_ALL) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherStart();
            }
        }
    }

    public void onLauncherStop() {
        if (LogUtils.DEBUG_ALL) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherStop();
            }
        }
    }

    public void onLauncherDestroy(Launcher launcher) {
        if (launcher != mLauncher) {
            LogUtils.w(TAG, "Launcher don't destroy. launcher:" + launcher + " mLauncher:" + mLauncher);
            return;
        }

        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME("launcher:" + launcher);
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherDestroy();
            }
        }
        mLauncher = null;
    }

    public void onLauncherRequestPermissionsResult(int requestCode, String[] permissions,
                                                   int[] grantResults) {
        DEBUG_PRINT_FUNCTIONNAME("rC:" + requestCode + " ret:" + Arrays.toString(grantResults));

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    public void onLauncherFocusChanged(boolean hasFocus) {
        if (LogUtils.DEBUG) {
            DEBUG_PRINT_FUNCTIONNAME("hasFocus:" + hasFocus);
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLauncherFocusChanged(hasFocus);
            }
        }
    }

    public void onReceiveHomeIntent() {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onHomeIntent();
            }
        }
    }

    public void onLauncherWorkspaceBindingFinish () {
        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onBindingWorkspaceFinish();
            }
        }
    }

    public void onLauncherAllAppBindingFinish (ArrayList<AppInfo> apps) {
        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME("AllApps size:"+apps.size());
        }
        if (LogUtils.DEBUG_ALL) {
            for (AppInfo app : apps) {
                LogUtils.d("Load app ", app.toComponentKey().toString() + "\n");
            }
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onBindingAllAppFinish(apps);
            }
        }
    }

    @Override
    public void onPackageRemoved(String packageName, UserHandle user) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME("pkgName:" + packageName +
                    " user:" + user.toString( ));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackageRemoved(packageName, user);
            }
        }
    }

    @Override
    public void onPackageAdded(String packageName, UserHandle user) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME("pkgName:" + packageName
                    + " user:" + user.toString( ));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackageAdded(packageName, user);
            }
        }
    }

    @Override
    public void onPackageChanged(String packageName, UserHandle user) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME("pkgName:" + packageName
                    + " user:" + user.toString( ));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackageChanged(packageName, user);
            }
        }
    }

    @Override
    public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME(
                    "packageNames:" + Arrays.toString(packageNames)
                            + " user:" + user.toString( ) + " replacing:" + replacing);
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesAvailable(packageNames, user, replacing);
            }
        }
    }

    @Override
    public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME(
                    "packageNames:" + Arrays.toString(packageNames)
                            + " user:" + user.toString( ) + " replacing:" + replacing);
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesUnavailable(packageNames, user, replacing);
            }
        }
    }

    @Override
    public void onPackagesSuspended(String[] packageNames, UserHandle user) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME(
                    "packageNames:" + Arrays.toString(packageNames) + " user:" + user.toString( ));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesSuspended(packageNames, user);
            }
        }
    }

    @Override
    public void onPackagesUnsuspended(String[] packageNames, UserHandle user) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME(
                    "packageNames:" + Arrays.toString(packageNames) + " user:" + user.toString( ));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPackagesUnsuspended(packageNames, user);
            }
        }
    }

    @Override
    public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME(
                    "packageNames" + packageName + " shortcuts:" + shortcuts.toString( )
                            + " user:" + user.toString( ));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onShortcutsChanged(packageName, shortcuts, user);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME("key:" + key);
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onAppSharedPreferenceChanged(key);
            }
        }
    }

    public void onModelReceive(Intent intent) {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME("action:" + intent.getAction( ));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onReceive(intent);
            }
        }
    }

    public void onAppCreated(Context context) {
        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onAppCreated(context);
            }
        }
    }

    public void onUIConfigChanged() {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUIConfigChanged();
            }
        }
    }

    public void onLauncherThemeChanged() {
        if (LogUtils.DEBUG_EXTERNAL_MSG) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onThemeChanged();
            }
        }
    }


    public void onAllAppsListUpdated(List<AppInfo> apps) {
        if (LogUtils.DEBUG_ALL) {
            DEBUG_PRINT_FUNCTIONNAME( );
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onAllAppsListUpdated(apps);
            }
        }
    }

    public void onLauncherConfigurationChanged(int diff) {
        boolean localChanged = (diff & CONFIG_LOCALE) != 0;
        boolean orientationChanged = (diff & CONFIG_ORIENTATION) != 0;
        boolean screensizeChanged = (diff & CONFIG_SCREEN_SIZE) != 0;
        DEBUG_PRINT_FUNCTIONNAME("local orientation screensize:"
                + localChanged + orientationChanged + screensizeChanged);
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                if (localChanged) {
                    cb.onLauncherLocaleChanged();
                }
                if (orientationChanged) {
                    cb.onLauncherOrientationChanged();
                }
                if (screensizeChanged) {
                    cb.onLauncherScreensizeChanged();
                }
            }
        }
    }

    public void onLoadAllAppsEnd (ArrayList<AppInfo> apps) {
        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME("AllApps size:"+apps.size());
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onLoadAllAppsEnd(apps);
            }
        }
    }

    public void onLauncherStyleChanged (String style) {
        if (LogUtils.DEBUG_LOADER) {
            DEBUG_PRINT_FUNCTIONNAME("style:"+style);
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onHomeStyleChanged(style);
            }
        }
    }

    public void onLauncherDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (LogUtils.DEBUG) {
            DEBUG_PRINT_FUNCTIONNAME("database version:" + oldVersion + " --> " + newVersion);
        }
        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.onDbUpgrade(db, oldVersion, newVersion);
            }
        }
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {
        boolean isAll = args.length > 0 && TextUtils.equals(args[0], "--all");

        if (mLauncher != null) {
            w.println();
            w.println(prefix + " DeviceProfile: " + InvariantDeviceProfile.INSTANCE.get(mLauncher));
            w.println();
            w.println(prefix + " WallpaperColorInfo: " + WallpaperColorInfo.getInstance(mLauncher));
        }

        for (int i = 0; i < mCallbacks.size(); i++) {
            LauncherAppMonitorCallback cb = mCallbacks.get(i).get();
            if (cb != null) {
                cb.dump(prefix, fd, w, isAll);
            }
        }

        if (mHotseatController != null) {
            mHotseatController.dumpState(prefix, fd, w, isAll);
        }

        if (mGesturesController != null) {
            mGesturesController.dumpState(prefix, fd, w, isAll);
        }

        if (FeatureFlags.ENABLE_DEBUG) {
            FeatureFlags.dumpState(w, isAll);
        }
    }

    public FolderIconController getFolderIconController() {
        return mFolderIconController;
    }

    public MultiModeController getMultiModeController() {
        return mMultiModeController;
    }

    public GesturesController getGesturesController() {
        return mGesturesController;
    }

    public CycleScrollController getCycleScrollController() {
        return mCycleScrollController;
    }

    public UnreadInfoController getUnreadInfoController() {
        return mUnreadInfoController;
    }

    public NotifyDotsNumController getNotifiDotsNumController() {
        return mNotifyDotsNumController;
    }

    public NavigationBarController getNavigationBarController() {
        return mNavigationBarController;
    }

    public CustomizeAppSortController getCustomizeAppSortController() {
        return mCustomizeAppSortController;
    }

    public DesktopGridController getDesktopGridController() {
        return mDesktopGridController;
    }

    public HotseatController getHotseatController() {
        return mHotseatController;
    }

    public DynamicIconController getDynamicIconController() {
        return mDynamicIconController;
    }

    public SRController getSRController() {
        return mSRController;
    }

    public IconLabelController getIconLabelController() {
        return mIconLabelController;
    }
}

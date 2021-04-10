package com.sprd.ext.multimode;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.sprd.ext.BaseController;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LauncherSettingsExtension;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import static android.app.ProgressDialog.show;
import static com.android.launcher3.LauncherProvider.SCHEMA_VERSION;

public class MultiModeController extends BaseController implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "MultiModeController";

    private static boolean sIsSupportDynamicChange;

    private static boolean sIsSingleLayerMode;

    private static boolean sIsDefaultMode;

    private static final HashSet<ShortcutKey> sPreModeShortcutKeys = new HashSet<>();

    public MultiModeController(Context context, LauncherAppMonitor monitor) {
        super(context);
        sIsSupportDynamicChange = MultiModeUtilities.isSupportDynamicHomeStyle(context);
        sIsSingleLayerMode = MultiModeUtilities.isSingleLayerMode(context);
        sIsDefaultMode = MultiModeUtilities.isDefaultMode(context);

        monitor.registerCallback(mAppMonitorCallback);
        LogUtils.d(TAG, this);
    }

    public static void initControllerIfNeeded(Context context) {
        if (FeatureOption.SPRD_MULTI_MODE_SUPPORT.get()
                && LauncherAppMonitor.getInstanceNoCreate() == null) {
            LauncherAppMonitor lam = LauncherAppMonitor.getInstance(context);
            if (lam != null && lam.getMultiModeController() != null) {
                LogUtils.d(TAG, "init success");
            }
        }
    }

    public static boolean isSupportDynamicChange() {
        throwIfControllerNotInited();
        return sIsSupportDynamicChange;
    }

    public static boolean isSingleLayerMode() {
        throwIfControllerNotInited();
        return sIsSingleLayerMode;
    }

    public static boolean isSingleLayerMode(Context context) {
        initControllerIfNeeded(context);
        return isSingleLayerMode();
    }

    public static boolean isDefaultMode() {
        throwIfControllerNotInited();
        return sIsDefaultMode;
    }

    public static HashSet<ShortcutKey> getPreModeSavedShortcuts() {
        throwIfControllerNotInited();
        return sPreModeShortcutKeys;
    }

    /**
     * We need unpin pre mode Shortcuts abd pinned cur mode Shortcuts before load workspace.
     */
    public static void restoreShortcutsIfNeeded(Context context) {
        // If version not support Shortcut.
        if (!DeepShortcutManager.supportsShortcuts(new SupportShortcutItemInfo())) {
            return;
        }
        throwIfControllerNotInited();
        if (sIsSupportDynamicChange) {
            MultiModeUtilities.restoreShortcuts(context, sPreModeShortcutKeys);
        }
    }

    private static void throwIfControllerNotInited() {
        if (FeatureOption.SPRD_MULTI_MODE_SUPPORT.get()) {
            LauncherAppMonitor Lam = LauncherAppMonitor.getInstanceNoCreate();
            if (Lam == null || Lam.getMultiModeController() == null) {
                throw new RuntimeException("MultiModeController is not init.");
            }
        }
    }

    private LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {

        @Override
        public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            MultiModeUtilities.updateSingleModeAppIconDb(mContext, db);
        }

        @Override
        public void onLoadAllAppsEnd(ArrayList<AppInfo> apps) {
            if (sIsSingleLayerMode) {
                LauncherModel.runOnWorkerThread(
                        new VerifyIdleAppTask(mContext, apps, null, null, false));
            }
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            if (sIsSingleLayerMode) {
                LauncherModel.runOnWorkerThread(
                        new VerifyIdleAppTask(mContext, null, packageName, user, true));
            }
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            if (sIsSingleLayerMode) {
                LauncherModel.runOnWorkerThread(
                        new VerifyIdleAppTask(mContext, null, packageName, user, false));
            }
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            w.println();
            w.println(prefix + TAG + ": " + MultiModeController.this);
        }
    };

    public static String getHomeScreenStylePrefValue(Context context) {
        return MultiModeUtilities.getHomeScreenStylePrefValue(context);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (LauncherSettingsExtension.sIsUserAMonkey) {
            return false;
        }

        final String newModel = (String) newValue;
        if (!getHomeScreenStylePrefValue(mContext).equals(newModel)) {
            // Value has changed
            show(preference.getContext(), null, mContext.getString(R.string.home_screen_style_notification),
                    true, false);

            LauncherModel.runOnWorkerThread(() -> {

                // Synchronously write the preference.
                MultiModeUtilities.syncSaveNewModel(mContext, newModel);

                LauncherAppMonitor.getInstance(mContext).onLauncherStyleChanged(newModel);

                LogUtils.d("Change Launcher style", "restarting launcher");
                UtilitiesExt.exitLauncher();
            });
        }
        return false;
    }

    public static String getKeyByMode(Context context, String originalKey) {
        return getKeyByMode(context, originalKey, SCHEMA_VERSION, isSingleLayerMode(context));
    }

    public static String getKeyByMode(
            Context context, String originalKey, int dbVersion, boolean isSingleLayerMode) {
        initControllerIfNeeded(context);
        return MultiModeUtilities.getKeyByMode(originalKey, isSingleLayerMode, dbVersion);
    }

    public static String getKeyByPreMode(Context context, String originalKey) {
        return getKeyByMode(context, originalKey, SCHEMA_VERSION, !isSingleLayerMode(context));
    }

    @VisibleForTesting
    public static void setSingleLayerMode(Context context, boolean isSingleMode) {
        MultiModeUtilities.syncSaveNewModel(context,
                isSingleMode ? MultiModeUtilities.SINGLE : MultiModeUtilities.DUAL);
        sIsSingleLayerMode = MultiModeUtilities.isSingleLayerMode(context);
        sIsDefaultMode = MultiModeUtilities.isDefaultMode(context);
    }

    @VisibleForTesting
    public static void setSupportDynamicChange(boolean isSupport) {
        sIsSupportDynamicChange = isSupport;
    }

    @NonNull
    @Override
    public String toString() {
        return "sIsSupportDynamicChange: " + sIsSupportDynamicChange
                + " sIsSingleLayerMode: " + sIsSingleLayerMode
                + " sIsDefaultMode: " + sIsDefaultMode;
    }

    /**
     * Create an ItemInfo always support Shortcut,
     * {@link com.android.launcher3.shortcuts.DeepShortcutManager#supportsShortcuts(ItemInfo)}
     * always return true.
     */
    private static class SupportShortcutItemInfo extends ItemInfo {
        SupportShortcutItemInfo() {
            itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        }

        @Override
        public boolean isDisabled() {
            return false;
        }
    }
}

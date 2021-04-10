package com.android.launcher3;

import android.content.Context;

import com.sprd.ext.multimode.MultiModeController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Central list of files the Launcher writes to the application data directory.
 *
 * To add a new Launcher file, create a String constant referring to the filename, and add it to
 * ALL_FILES, as shown below.
 */
public class LauncherFiles {

    private static final String XML = ".xml";

    public static final String SHARED_PREFERENCES_KEY = "com.android.launcher3.prefs";
    public static final String MANAGED_USER_PREFERENCES_KEY = "com.android.launcher3.managedusers.prefs";
    // This preference file is not backed up to cloud.
    public static final String DEVICE_PREFERENCES_KEY = "com.android.launcher3.device.prefs";

    private static final String PREFIX = "sl_";
    private static final String LAUNCHER_DB = "launcher.db";
    private static final String SL_LAUNCHER_DB = PREFIX + LAUNCHER_DB;
    private static final String WIDGET_PREVIEWS_DB = "widgetpreviews.db";
    private static final String SL_WIDGET_PREVIEWS_DB = PREFIX + WIDGET_PREVIEWS_DB;
    private static final String APP_ICONS_DB = "app_icons.db";
    private static final String SL_APP_ICONS_DB = PREFIX + APP_ICONS_DB;

    private enum DbType {
        LAUNCHER,
        WIDGET_PREVIEWS,
        APP_ICONS
    }

    public static final List<String> ALL_FILES = Collections.unmodifiableList(Arrays.asList(
            LAUNCHER_DB,
            SL_LAUNCHER_DB,
            SHARED_PREFERENCES_KEY + XML,
            WIDGET_PREVIEWS_DB,
            SL_WIDGET_PREVIEWS_DB,
            MANAGED_USER_PREFERENCES_KEY + XML,
            DEVICE_PREFERENCES_KEY + XML,
            APP_ICONS_DB,
            SL_APP_ICONS_DB));

    public static String getLauncherDb(Context context) {
        return getLauncherDb(MultiModeController.isSingleLayerMode(context));
    }

    public static String getLauncherNonCurModeDb(Context context) {
        return getLauncherDb(!MultiModeController.isSingleLayerMode(context));
    }

    public static String getLauncherDb(boolean isSingleMode) {
        return getDbByMode(DbType.LAUNCHER, isSingleMode);
    }

    static String getWidgetPreviewsDB(Context context) {
        return getDbByMode(DbType.WIDGET_PREVIEWS, MultiModeController.isSingleLayerMode(context));
    }

    public static String getAppIconsDb(Context context) {
        return getDbByMode(DbType.APP_ICONS, MultiModeController.isSingleLayerMode(context));
    }

    private static String getDbByMode(DbType type, boolean isSingleLayMode) {
        boolean isDynamicSingleLayer = MultiModeController.isSupportDynamicChange() && isSingleLayMode;
        switch (type) {
            case LAUNCHER:
                return isDynamicSingleLayer ? SL_LAUNCHER_DB : LAUNCHER_DB;
            case WIDGET_PREVIEWS:
                return isDynamicSingleLayer ? SL_WIDGET_PREVIEWS_DB : WIDGET_PREVIEWS_DB;
            case APP_ICONS:
                return isDynamicSingleLayer ? SL_APP_ICONS_DB : APP_ICONS_DB;
            default:
                throw new RuntimeException("get db name by mode type is error!");
        }

    }
}

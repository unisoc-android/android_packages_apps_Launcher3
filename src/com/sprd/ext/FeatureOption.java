package com.sprd.ext;

import android.util.Log;

import com.android.launcher3.config.BaseFlags.TogglableFlag;

/**
 * Defines a set of features used to control launcher feature.
 * All the feature should be defined here with appropriate default values.
 * This class is kept package-public to direct access.
 */

public final class FeatureOption {
    private static final String TAG = "FeatureOption";

    public static final TogglableFlag SPRD_MULTI_MODE_SUPPORT = new TogglableFlag(
            "SPRD_MULTI_MODE_SUPPORT", getProp("ro.launcher.multimode"),
            "enable user can select user aosp mode or singlelayer mode");

    // When enabled allows customization of the columns and rows on the desktop.
    public static final TogglableFlag SPRD_DESKTOP_GRID_SUPPORT = new TogglableFlag(
            "SPRD_DESKTOP_GRID_SUPPORT", getProp("ro.launcher.desktopgrid"),
            "enable allows customization of the columns and rows on the desktop");

    static final TogglableFlag SPRD_NOTIFICATION_DOT_COUNT = new TogglableFlag(
            "SPRD_NOTIFICATION_DOT_COUNT", getProp("ro.launcher.notifbadge.count"),
            "enable show the notification badge count");

    static final TogglableFlag SPRD_BADGE_SUPPORT = new TogglableFlag(
            "SPRD_BADGE_SUPPORT", getProp("ro.launcher.badge"),
            "enable the phone & message & unisoc email & unisoc calendar " +
                    "icon will show icon badge");
    public static final boolean SPRD_BADGE_PHONE_SUPPORT =
            getProp("ro.launcher.badge.phone", true);
    public static final boolean SPRD_BADGE_MESSAGE_SUPPORT =
            getProp("ro.launcher.badge.message", true);
    public static final boolean SPRD_BADGE_EMAIL_SUPPORT =
            getProp("ro.launcher.badge.email", true);
    public static final boolean SPRD_BADGE_CALENDAR_SUPPORT =
            getProp("ro.launcher.badge.calendar", true);

    public static final TogglableFlag SPRD_DYNAMIC_ICON_SUPPORT = new TogglableFlag(
            "SPRD_DYNAMIC_ICON_SUPPORT", getProp("ro.launcher.dynamic"),
            "enable the clock & calendar icon will dynamic update");
    public static final boolean SPRD_DYNAMIC_ICON_CLOCK_SUPPORT =
            getProp("ro.launcher.dynamic.clock", true);
    public static final boolean SPRD_DYNAMIC_ICON_CALENDAR_SUPPORT =
            getProp("ro.launcher.dynamic.calendar", true);

    public static final TogglableFlag SPRD_DISABLE_ROTATION = new TogglableFlag(
            "SPRD_DISABLE_ROTATION",
            getProp("ro.launcher.disable.rotation", UtilitiesExt.IS_LOW_RAM),
            "enable will hide rotation menu item on setting activity");


    public static final boolean SPRD_GESTURE_ENABLE = getProp("ro.launcher.gesture", true);
    public static final TogglableFlag SPRD_GESTURE_SUPPORT = new TogglableFlag(
            "SPRD_GESTURE_SUPPORT", SPRD_GESTURE_ENABLE,
            "enable will support workspace Gestures");

    public static final TogglableFlag SPRD_CYCLE_SCROLL_SUPPORT = new TogglableFlag(
            "SPRD_CYCLE_SCROLL_SUPPORT", getProp("ro.launcher.cyclescroll", true),
            "enable workspace will cycle scroll");

    public static final TogglableFlag SPRD_FOLDER_ICON_MODE_SUPPORT = new TogglableFlag(
            "SPRD_FOLDER_ICON_MODE_SUPPORT", getProp("ro.launcher.foldericonmode",
            SPRD_MULTI_MODE_SUPPORT.get()),
            "enable folder icon support grid mode & aosp mode");

    public static final TogglableFlag SPRD_HOTSEAT_ICON_ADAPTIVE_LAYOUT = new TogglableFlag(
            "SPRD_HOTSEAT_ICON_ADAPTIVE_LAYOUT", getProp("ro.launcher.hs.adaptive",
            SPRD_DESKTOP_GRID_SUPPORT.get()),
            "enable the hotseat icon will adaptive layout");

    public static final TogglableFlag SPRD_ALLAPP_CUSTOMIZE_SUPPORT = new TogglableFlag(
            "SPRD_ALLAPP_CUSTOMIZE_SUPPORT", getProp("ro.launcher.allapp.customize"),
            "enable can customize the allapp views app position");


    public static final TogglableFlag SPRD_TASK_LOCK_SUPPORT = new TogglableFlag(
            "SPRD_TASK_LOCK_SUPPORT", getProp("ro.launcher.tasklock", true),
            "only for recent:enable lock task");

    public static final TogglableFlag SPRD_SHOW_MEMINFO_SUPPORT = new TogglableFlag(
            "SPRD_SHOW_MEMINFO_SUPPORT", getProp("ro.launcher.showmeminfo", true),
            "only for recent:enable show memory info");

    public static final TogglableFlag SPRD_CLEAR_ALL_ON_BOTTOM_SUPPORT = new TogglableFlag(
            "SPRD_CLEAR_ALL_ON_BOTTOM_SUPPORT", getProp("ro.launcher.clearallonbottom", true),
            "only for recent:enable show clear all button under the recent task list");

    public static final TogglableFlag SPRD_SHOW_CLEAR_MEM_SUPPORT = new TogglableFlag(
            "SPRD_SHOW_CLEAR_MEM_SUPPORT", getProp("ro.launcher.showclearmem", true),
            "only for recent:enable show toast when clear mem");

    public static final TogglableFlag SPRD_APP_REMOTE_ANIM_SUPPORT = new TogglableFlag(
            "SPRD_APP_REMOTE_ANIM_SUPPORT", getProp("sys.app.remote.anim"),
            "Performance features: remote animation for app");


    public static final TogglableFlag SPRD_FAST_UPDATE_LABEL = new TogglableFlag(
            "SPRD_ALLAPP_FUZZY_SEARCH_SUPPORT",
            getProp("ro.launcher.label.fastupdate", true),
            "Performance features:fast update label when language changing");

    public static final TogglableFlag SPRD_ALLAPP_FUZZY_SEARCH_SUPPORT = new TogglableFlag(
            "SPRD_ALLAPP_FUZZY_SEARCH_SUPPORT",
            getProp("ro.launcher.allapp.fuzzysearch", !UtilitiesExt.IS_LOW_RAM),
            "enable all apps searchbox can search any character");

    public static final TogglableFlag SPRD_DEBUG_SEARCH_CUSTOMIZE_SUPPORT = new TogglableFlag(
            "SPRD_DEBUG_SEARCH_CUSTOMIZE_SUPPORT", false,
            "Replace the search box with other search box");

    public static final TogglableFlag SPRD_ALLAPP_BG_TRANSPARENT_SUPPORT = new TogglableFlag(
            "SPRD_ALLAPP_BG_TRANSPARENT_SUPPORT", getProp("ro.launcher.allapp.bgtransp"),
            "enable can customize the allapps background's alpha");

    public static final TogglableFlag SPRD_ICON_LABEL_LINE_SUPPORT = new TogglableFlag(
            "SPRD_ICON_LABEL_LINE_SUPPORT", getProp("ro.launcher.iconlabelline"),
            "icon label support display double lines.");

    private static boolean getProp(String prop) {
        return getProp(prop, false);
    }

    private static boolean getProp(String prop, boolean defaultValue) {
        boolean ret = false;

        try {
            ret = SystemPropertiesUtils.getBoolean(prop, defaultValue);
        } catch (Exception e) {
            LogUtils.e(TAG, "getProp:" + prop + " error." + e);
        }

        return ret;
    }

    /**
     * To enable debug feature, execute the following command:
     * $ adb shell setprop log.tag.FeatureOption DEBUG
     */
    public static boolean enableDebugFeatures() {
        return Log.isLoggable(TAG, Log.DEBUG);
    }
}

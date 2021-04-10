package com.sprd.ext.dynamicicon;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.launcher3.Utilities;
import com.sprd.ext.FeatureOption;

import java.util.Calendar;
import java.util.Locale;

public class DynamicIconUtils {

    public static boolean anyDynamicIconSupport() {
        if (FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT.get()) {
            return FeatureOption.SPRD_DYNAMIC_ICON_CALENDAR_SUPPORT
                    || FeatureOption.SPRD_DYNAMIC_ICON_CLOCK_SUPPORT;
        }
        return false;
    }

    static void setAppliedValue(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putBoolean(key, value).apply();
    }

    public static boolean getAppliedValue(Context context, String key, boolean def) {
        SharedPreferences sharedPref = Utilities.getPrefs(context);
        return sharedPref.getBoolean(key, def);
    }

    static void removePrefKeyFromSharedPref(Context context, String... keys) {
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.apply();
    }

    public static int dayOfMonth() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    public static String dayOfWeek() {
        String day = Calendar.getInstance().getDisplayName(
                Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
        return day == null ? "" : day.toUpperCase();
    }

    public static int timeOfField(int field) {
        return Calendar.getInstance().get(field);
    }
}

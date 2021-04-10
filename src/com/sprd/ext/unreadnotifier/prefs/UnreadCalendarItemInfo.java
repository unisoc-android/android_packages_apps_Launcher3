package com.sprd.ext.unreadnotifier.prefs;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.text.TextUtils;

import com.android.launcher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SystemPropertiesUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;
import com.sprd.ext.unreadnotifier.UnreadSettingsActivity;

import java.util.ArrayList;

public class UnreadCalendarItemInfo extends UnreadBaseItemInfo {
    private static final String TAG = "UnreadCalendarItemInfo";
    private static final Uri CALENDARS_CONTENT_URI = CalendarContract.CalendarAlerts.CONTENT_URI;

    private static final String PROP_DEFAULT_CALENDAR = "ro.launcher.unread.calendar";

    public UnreadCalendarItemInfo(Context context, UnreadInfoManager unreadInfoManager) {
        super(context, unreadInfoManager,
                UnreadInfoManager.TYPE_CALENDAR,
                UnreadSettingsActivity.UnreadSettingsFragment.PREF_KEY_UNREAD_CALENDAR,
                CALENDARS_CONTENT_URI,
                Manifest.permission.READ_CALENDAR,
                ComponentName.unflattenFromString(context.getString(R.string.unread_default_calendar_cn)));
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_calendar_enable);
        mDefaultState = SystemPropertiesUtils.getBoolean(PROP_DEFAULT_CALENDAR, defaultState);
    }

    @Override
    public int readUnreadCount() {
        String[] ALERT_PROJECTION = new String[]{CalendarAlerts._ID, // 0
                CalendarAlerts.EVENT_ID, // 1
                CalendarAlerts.STATE, // 2
                CalendarAlerts.TITLE, // 3
                CalendarAlerts.EVENT_LOCATION, // 4
                CalendarAlerts.SELF_ATTENDEE_STATUS, // 5
                CalendarAlerts.ALL_DAY, // 6
                CalendarAlerts.ALARM_TIME, // 7
                CalendarAlerts.MINUTES, // 8
                CalendarAlerts.BEGIN, // 9
                CalendarAlerts.END, // 10
                CalendarAlerts.DESCRIPTION, // 11
        };
        int unreadEvents = 0;

        boolean result = checkPermission();
        if (!result) {
            LogUtils.w(TAG, "no READ_CALENDAR Permission");
            return 0;
        }
        ContentResolver resolver = mContext.getContentResolver();
        Cursor alertCursor = null;
        try {
            alertCursor = resolver
                    .query(CALENDARS_CONTENT_URI,
                            ALERT_PROJECTION,
                            ("(" + CalendarAlerts.STATE + "=? OR "
                                    + CalendarAlerts.STATE + "=?) AND "
                                    + CalendarAlerts.ALARM_TIME + "<=" + System
                                    .currentTimeMillis()),
                            new String[]{
                                    Integer.toString(CalendarAlerts.STATE_FIRED),
                                    Integer.toString(CalendarAlerts.STATE_SCHEDULED)},
                            "begin DESC, end DESC");
            if (alertCursor != null) {
                unreadEvents = alertCursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
            LogUtils.d(TAG, "readUnreadCount Exception: " + e);
        } finally {
            UtilitiesExt.closeCursorSilently(alertCursor);
        }

        if (LogUtils.DEBUG_UNREAD)
            LogUtils.d(TAG, "readUnreadCount, unread Calendar num = " + unreadEvents);

        return unreadEvents;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_calendar);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        String[] calLists = context.getResources().getStringArray(R.array.support_calendar_component_array);

        ArrayList<String> installedCalendarList = new ArrayList<>();
        for (String calList : calLists) {
            if (!TextUtils.isEmpty(calList)) {
                ComponentName componentName = ComponentName.unflattenFromString(calList);
                if (componentName != null) {
                    boolean isInstalled = UtilitiesExt.isAppInstalled(context,
                            componentName.getPackageName(), android.os.Process.myUserHandle());
                    if (isInstalled) {
                        installedCalendarList.add(componentName.flattenToShortString());
                    }
                }
            }
        }

        return installedCalendarList;
    }
}

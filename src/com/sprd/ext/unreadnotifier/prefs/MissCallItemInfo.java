package com.sprd.ext.unreadnotifier.prefs;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog;

import com.android.launcher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SystemPropertiesUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;
import com.sprd.ext.unreadnotifier.UnreadSettingsActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MissCallItemInfo extends UnreadBaseItemInfo {
    private static final String TAG = "MissCallItemInfo";
    private static final Uri CALLS_CONTENT_URI = CallLog.Calls.CONTENT_URI;
    private static final String MISSED_CALLS_SELECTION =
            CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.IS_READ + " = 0";

    private static final String PROP_DEFAULT_CALL = "ro.launcher.unread.call";

    public MissCallItemInfo(Context context, UnreadInfoManager unreadInfoManager) {
        super(context, unreadInfoManager,
                UnreadInfoManager.TYPE_CALL_LOG,
                UnreadSettingsActivity.UnreadSettingsFragment.PREF_KEY_MISS_CALL,
                CALLS_CONTENT_URI,
                Manifest.permission.READ_CALL_LOG,
                ComponentName.unflattenFromString(context.getString(R.string.unread_default_call_cn)));
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_call_enable);
        mDefaultState = SystemPropertiesUtils.getBoolean(PROP_DEFAULT_CALL, defaultState);
    }

    @Override
    public int readUnreadCount() {
        int missedCalls = 0;
        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            LogUtils.w(TAG, "no READ_CALL_LOG Permission");
            return 0;
        }

        Cursor cursor = null;
        try {
            cursor = resolver.query(CALLS_CONTENT_URI, new String[]{BaseColumns._ID},
                    MISSED_CALLS_SELECTION, null, null);
            if (cursor != null) {
                missedCalls = cursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
            LogUtils.d(TAG, "readUnreadCount Exception: " + e);
        } finally {
            UtilitiesExt.closeCursorSilently(cursor);
        }

        if (LogUtils.DEBUG_UNREAD) LogUtils.d(TAG, "readUnreadCount, missedCalls = " + missedCalls);

        return missedCalls;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_call);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        ArrayList<String> arrayList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        // Get the list of apps registered for the DIAL intent with empty scheme
        Intent intent = new Intent(Intent.ACTION_DIAL);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
        if (null == resolveInfoList) return arrayList;

        List<String> callPackageNames = new ArrayList<>();

        for (ResolveInfo resolveInfo : resolveInfoList) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && !callPackageNames.contains(activityInfo.packageName)) {
                callPackageNames.add(activityInfo.packageName);
            }
        }

        Intent intentAll = new Intent(Intent.ACTION_MAIN);
        intentAll.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allResolveInfoList = packageManager.queryIntentActivities(intentAll, 0);
        if (null == allResolveInfoList) return arrayList;

        HashMap<String, ComponentName> callActivityInfo = new HashMap<>();

        // Add one entry to the map for every sms Application
        for (ResolveInfo resolveInfo : allResolveInfoList) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null) {
                continue;
            }
            final String packageName = activityInfo.packageName;
            if (callPackageNames.contains(packageName)) {
                if (!callActivityInfo.containsKey(packageName)) {
                    final ComponentName callCn = new ComponentName(packageName, activityInfo.name);
                    callActivityInfo.put(packageName, callCn);
                    arrayList.add(callCn.flattenToString());
                }
            }
        }
        return arrayList;
    }
}
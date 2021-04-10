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

import com.android.launcher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SystemPropertiesUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.sprd.ext.unreadnotifier.UnreadSettingsActivity.UnreadSettingsFragment.PREF_KEY_UNREAD_SMS;

public class UnreadMessageItemInfo extends UnreadBaseItemInfo {
    private static final String TAG = "UnreadMessageItemInfo";
    private static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri MMSSMS_CONTENT_URI = Uri.parse("content://mms-sms");

    private static final String PROP_DEFAULT_SMS = "ro.launcher.unread.sms";

    public UnreadMessageItemInfo(Context context, UnreadInfoManager unreadInfoManager) {
        super(context, unreadInfoManager,
                UnreadInfoManager.TYPE_SMS,
                PREF_KEY_UNREAD_SMS,
                MMSSMS_CONTENT_URI,
                Manifest.permission.READ_SMS,
                ComponentName.unflattenFromString(context.getString(R.string.unread_default_sms_cn)));
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_sms_enable);
        mDefaultState = SystemPropertiesUtils.getBoolean(PROP_DEFAULT_SMS, defaultState);
    }

    @Override
    public int readUnreadCount() {
        int unreadSms = 0;
        int unreadMms = 0;
        ContentResolver resolver = mContext.getContentResolver();

        if (!checkPermission()) {
            LogUtils.w(TAG, "no READ_SMS Permission");
            return 0;
        }

        Cursor smsCursor = null;
        try {
            smsCursor = resolver.query(SMS_CONTENT_URI, new String[]{BaseColumns._ID},
                    "type = 1 AND read = 0", null, null);
            if (smsCursor != null) {
                unreadSms = smsCursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
            LogUtils.d(TAG, "readUnreadCount SMS Exception: " + e);
        } finally {
            UtilitiesExt.closeCursorSilently(smsCursor);
        }

        Cursor mmsCursor = null;
        try {
            mmsCursor = resolver.query(MMS_CONTENT_URI, new String[]{BaseColumns._ID},
                    "msg_box = 1 AND read = 0 AND ( m_type =130 OR m_type = 132 ) AND thread_id > 0",
                    null, null);
            if (mmsCursor != null) {
                unreadMms = mmsCursor.getCount();
            }
        } catch (Exception e) {
            // TODO: handle exception
            LogUtils.d(TAG, "readUnreadCount MMS Exception: " + e);
        } finally {
            UtilitiesExt.closeCursorSilently(mmsCursor);
        }

        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "readUnreadCount: unread [sms : mms] = ["
                    + unreadSms + " : " + unreadMms + "]");
        }

        return unreadMms + unreadSms;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_sms);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        ArrayList<String> arrayList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        // Get the list of apps registered for SMS
        Intent smsIntent = new Intent("android.provider.Telephony.SMS_DELIVER");
        List<ResolveInfo> smsReceivers = packageManager.queryBroadcastReceivers(smsIntent, 0);

        // Get package names for all SMS apps
        List<String> smsPackageNames = new ArrayList<>();

        for (ResolveInfo resolveInfo : smsReceivers) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null) {
                continue;
            }
            if (!Manifest.permission.BROADCAST_SMS.equals(activityInfo.permission)) {
                continue;
            }
            if (!smsPackageNames.contains(activityInfo.packageName)) {
                smsPackageNames.add(activityInfo.packageName);
            }
        }

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allResolveInfoList = packageManager.queryIntentActivities(intent, 0);
        if (null == allResolveInfoList) return arrayList;

        HashMap<String, ComponentName> smsActivityInfo = new HashMap<>();

        // Add one entry to the map for every sms Application
        for (ResolveInfo resolveInfo : allResolveInfoList) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null) {
                continue;
            }
            final String packageName = activityInfo.packageName;
            if (smsPackageNames.contains(packageName)) {
                if (!smsActivityInfo.containsKey(packageName)) {
                    final ComponentName smsCn = new ComponentName(packageName, activityInfo.name);
                    smsActivityInfo.put(packageName, smsCn);
                    arrayList.add(smsCn.flattenToString());
                }
            }
        }
        return arrayList;
    }
}




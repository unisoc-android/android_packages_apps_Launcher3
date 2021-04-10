package com.sprd.ext.unreadnotifier.prefs;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.android.launcher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.SystemPropertiesUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;

import java.util.ArrayList;

import static com.sprd.ext.unreadnotifier.UnreadSettingsActivity.UnreadSettingsFragment.PREF_KEY_UNREAD_EMAIL;

public class UnreadEmailItemInfo extends UnreadBaseItemInfo {
    private static final String TAG = "UnreadEmailItemInfo";
    private static final Uri EMAILS_CONTENT_URI = Uri.parse("content://com.android.email.provider/mailbox");
    private static final Uri EMAILS_NOTIFY_URI = Uri.parse("content://com.android.email.notifier");

    private static final String EMAILS_PERMISSION = "com.android.email.permission.ACCESS_PROVIDER";

    private static final String PROP_DEFAULT_EMAIL = "ro.launcher.unread.email";

    public UnreadEmailItemInfo(Context context, UnreadInfoManager unreadInfoManager) {
        super(context, unreadInfoManager,
                UnreadInfoManager.TYPE_EMAIL,
                PREF_KEY_UNREAD_EMAIL,
                EMAILS_NOTIFY_URI,
                EMAILS_PERMISSION,
                ComponentName.unflattenFromString(context.getString(R.string.unread_default_email_cn)));
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_email_enable);
        mDefaultState = SystemPropertiesUtils.getBoolean(PROP_DEFAULT_EMAIL, defaultState);
    }

    @Override
    public int readUnreadCount() {
        int unreadEmail = 0;
        int unRead;

        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            LogUtils.w(TAG, "no EMAILS_PERMISSION Permission");
            return 0;
        }

        Cursor cursor = null;
        try {
            cursor = resolver.query(EMAILS_CONTENT_URI, new String[]{"unreadCount"},
                    "type = ?", new String[]{"0"}, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    unRead = cursor.getInt(0);
                    if (unRead > 0) {
                        unreadEmail += unRead;
                    }
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            LogUtils.d(TAG, "readUnreadCount Exception: " + e);
        } finally {
            UtilitiesExt.closeCursorSilently(cursor);
        }

        if (LogUtils.DEBUG_UNREAD) LogUtils.d(TAG, "readUnreadCount: unreadEmail = " + unreadEmail);

        return unreadEmail;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_email);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        String[] emailLists = context.getResources().getStringArray(R.array.support_email_component_array);

        ArrayList<String> installEmailList = new ArrayList<>();
        for (String emailList : emailLists) {
            if (!TextUtils.isEmpty(emailList)) {
                ComponentName componentName = ComponentName.unflattenFromString(emailList);
                if (null != componentName) {
                    boolean isInstalled = UtilitiesExt.isAppInstalled(context,
                            componentName.getPackageName(), android.os.Process.myUserHandle());
                    if (isInstalled) {
                        installEmailList.add(componentName.flattenToShortString());
                    }
                }
            }
        }

        return installEmailList;
    }
}

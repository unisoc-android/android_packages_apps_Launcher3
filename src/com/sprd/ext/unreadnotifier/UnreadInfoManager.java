package com.sprd.ext.unreadnotifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.launcher3.Launcher;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LogUtils;
import com.sprd.ext.unreadnotifier.prefs.MissCallItemInfo;
import com.sprd.ext.unreadnotifier.prefs.UnreadBaseItemInfo;
import com.sprd.ext.unreadnotifier.prefs.UnreadCalendarItemInfo;
import com.sprd.ext.unreadnotifier.prefs.UnreadEmailItemInfo;
import com.sprd.ext.unreadnotifier.prefs.UnreadMessageItemInfo;

import java.util.ArrayList;

public class UnreadInfoManager {
    private static final String TAG = "UnreadInfoManager";

    public static final int TYPE_CALL_LOG = 101;
    public static final int TYPE_SMS = 102;
    public static final int TYPE_EMAIL = 103;
    public static final int TYPE_CALENDAR = 104;
    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private static final ArrayList<UnreadBaseItemInfo> ALL_ITEMS =
            new ArrayList<>();
    private static final ArrayList<UnreadBaseItemInfo> ALL_GRANTEDPERMISSION_ITEMS =
            new ArrayList<>();
    private static final ArrayList<UnreadBaseItemInfo> ALL_DENIEDPERMISSION_ITEMS =
            new ArrayList<>();

    private Context mContext;
    private UnreadMessageItemInfo mMessageUnreadItemInfo;
    private MissCallItemInfo mMissCallItemInfo;
    private UnreadEmailItemInfo mUnreadEmailItemInfo;
    private UnreadCalendarItemInfo mUnreadCalendarItemInfo;

    UnreadInfoManager(Context context) {
        mContext = context;
    }

    void createItemIfNeeded() {
        if (ALL_ITEMS.isEmpty()) {
            if (LogUtils.DEBUG_UNREAD) {
                LogUtils.d(TAG, " ALL_ITEMS is empty, need create items.");
            }
            createItems();
        }
    }

    private void createItems() {
        // init begin
        if (FeatureOption.SPRD_BADGE_MESSAGE_SUPPORT && mMessageUnreadItemInfo == null) {
            mMessageUnreadItemInfo = new UnreadMessageItemInfo(mContext, this);
            ALL_ITEMS.add(mMessageUnreadItemInfo);
        }

        if (FeatureOption.SPRD_BADGE_PHONE_SUPPORT && mMissCallItemInfo == null) {
            mMissCallItemInfo = new MissCallItemInfo(mContext, this);
            ALL_ITEMS.add(mMissCallItemInfo);
        }

        if (FeatureOption.SPRD_BADGE_EMAIL_SUPPORT && mUnreadEmailItemInfo == null) {
            mUnreadEmailItemInfo = new UnreadEmailItemInfo(mContext, this);
            ALL_ITEMS.add(mUnreadEmailItemInfo);
        }

        if (FeatureOption.SPRD_BADGE_CALENDAR_SUPPORT && mUnreadCalendarItemInfo == null) {
            mUnreadCalendarItemInfo = new UnreadCalendarItemInfo(mContext, this);
            ALL_ITEMS.add(mUnreadCalendarItemInfo);
        }

        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "createItems(), size of ALL_ITEMS is " + ALL_ITEMS.size());
        }
    }

    void initAppsAndPermissionList() {
        int N = ALL_ITEMS.size();

        ALL_GRANTEDPERMISSION_ITEMS.clear();
        ALL_DENIEDPERMISSION_ITEMS.clear();

        for (int i = 0; i < N; i++) {
            UnreadBaseItemInfo item = ALL_ITEMS.get(i);

            // verify ComponentName
            ArrayList<String> listValues = item.loadApps(mContext);
            item.verifyDefaultCN(listValues, item.defaultCn);
            item.setInstalledList(listValues);

            // init permission List
            if (item.isPersistChecked()) {
                if (item.checkPermission()) {
                    ALL_GRANTEDPERMISSION_ITEMS.add(item);
                } else {
                    ALL_DENIEDPERMISSION_ITEMS.add(item);
                }
            }
        }
    }

    void initUnreadInfo() {
        int N = ALL_DENIEDPERMISSION_ITEMS.size();

        String[] deniedString = new String[N];
        for (int i = 0; i < N; i++) {
            deniedString[i] = ALL_DENIEDPERMISSION_ITEMS.get(i).permission;
        }
        if (N > 0) {
            Launcher launcher = LauncherAppMonitor.getInstanceNoCreate().getLauncher();
            if (launcher != null) {
                launcher.requestPermissions(deniedString, PERMISSIONS_REQUEST_CODE);
            }
        }

        N = ALL_GRANTEDPERMISSION_ITEMS.size();
        for (int i = 0; i < N; i++) {
            UnreadBaseItemInfo item = ALL_GRANTEDPERMISSION_ITEMS.get(i);
            if (item != null) {
                item.contentObserver.registerContentObserver();
                item.updateUIFromDatabase();
            }
        }
    }

    boolean isDeniedPermissionItem(String key) {
        int N = ALL_DENIEDPERMISSION_ITEMS.size();
        for (int i = 0; i < N; i++) {
            UnreadBaseItemInfo item = ALL_DENIEDPERMISSION_ITEMS.get(i);
            if (item != null && item.getCurrentComponentName() != null) {
                String value = item.getCurrentComponentName().flattenToShortString();
                if (value.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public UnreadBaseItemInfo getItemByType(int type) {
        for (int i = 0; i < ALL_ITEMS.size(); i++) {
            UnreadBaseItemInfo item = ALL_ITEMS.get(i);
            if (item.type == type) {
                return item;
            }
        }
        return null;
    }

    UnreadBaseItemInfo getItemByKey(String key) {
        for (int i = 0; i < ALL_ITEMS.size(); i++) {
            UnreadBaseItemInfo item = ALL_ITEMS.get(i);
            if (item.prefKey.equals(key)) {
                return item;
            }
        }
        return null;
    }

    void handleRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "handleRequestPermissionResult, onPermissionsResult counts: "
                    + permissions.length + ":" + grantResults.length);
        }

        ArrayList<UnreadBaseItemInfo> grantedList = new ArrayList<>();
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                for (UnreadBaseItemInfo item : ALL_DENIEDPERMISSION_ITEMS) {
                    if (item.permission.equals(permissions[i])) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            if (LogUtils.DEBUG_UNREAD) {
                                LogUtils.d(TAG, "handleRequestPermissionResult, permission granted:" + item.permission);
                            }
                            item.contentObserver.registerContentObserver();
                            item.updateUIFromDatabase();
                            grantedList.add(item);
                        } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(mContext, item.getUnreadHintString(), Toast.LENGTH_LONG).show();
                            if (LogUtils.DEBUG_UNREAD) {
                                LogUtils.d(TAG, "handleRequestPermissionResult, permission denied:" + item.permission);
                            }
                        }
                    }
                }
            }
        } else {
            for (UnreadBaseItemInfo item : ALL_DENIEDPERMISSION_ITEMS) {
                if (item.checkPermission()) {
                    if (LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "handleRequestPermissionResult, permission granted for other requestCode:" + item.permission);
                    }
                    item.contentObserver.registerContentObserver();
                    item.updateUIFromDatabase();
                    grantedList.add(item);
                }
            }
        }
        // update all granted items
        for (UnreadBaseItemInfo item : grantedList) {
            ALL_DENIEDPERMISSION_ITEMS.remove(item);
            ALL_GRANTEDPERMISSION_ITEMS.add(item);
        }
    }

    public void updateUI(final Context context, final String desComponentName) {
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateUI, desComponentName = " + desComponentName);
        }
        if (!TextUtils.isEmpty(desComponentName)) {
            ComponentName cmpName = ComponentName.unflattenFromString(desComponentName);
            int unreadCount = getUnreadCountForDesComponent(cmpName);
            UnreadInfoController uc = LauncherAppMonitor.getInstance(context).getUnreadInfoController();
            if (null != uc) {
                uc.getUnreadHelper().updateComponentUnreadInfo(unreadCount, cmpName, Process.myUserHandle());
            }
        }
    }

    private int getUnreadCountForDesComponent(final ComponentName desComponentName) {
        int result = 0;
        for (UnreadBaseItemInfo item : ALL_ITEMS) {
            if (!TextUtils.isEmpty(item.currentCn)) {
                ComponentName cmpName = ComponentName.unflattenFromString(item.currentCn);
                if (cmpName.equals(desComponentName)) {
                    result += item.getUnreadCount();
                }
            }
        }
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "getUnreadCountForDesComponent, unreadCount of desComponentName: "
                    + desComponentName + " is: " + result);
        }
        return result;
    }

    void unregisterItemContentObservers() {
        int N = ALL_ITEMS.size();
        for (int i = 0; i < N; i++) {
            UnreadBaseItemInfo item = ALL_ITEMS.get(i);
            if (item != null) {
                item.contentObserver.unregisterContentObserver();
            }
        }
    }

}

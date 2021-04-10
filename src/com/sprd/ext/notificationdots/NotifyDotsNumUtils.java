package com.sprd.ext.notificationdots;

import android.content.Context;

import com.sprd.ext.LauncherAppMonitor;

public class NotifyDotsNumUtils {

    public static void setNotifyDotsExtPrefEnabled(Context context, boolean enable) {
        NotifyDotsNumController ndnc = LauncherAppMonitor.getInstance(context).getNotifiDotsNumController();
        if (null != ndnc) {
            ndnc.setNotifyDotsExtPrefEnabled(enable);
        }
    }

    public static boolean isFullFreshEnabled(Context context) {
        boolean fullFresh = false;
        NotifyDotsNumController ndnc = LauncherAppMonitor.getInstance(context).getNotifiDotsNumController();
        if (ndnc != null) {
            fullFresh = ndnc.isFullFreshEnabled();
        }
        return fullFresh;
    }

    public static void setFullFreshEnabled(Context context, boolean enabled) {
        NotifyDotsNumController ndnc = LauncherAppMonitor.getInstance(context).getNotifiDotsNumController();
        if (ndnc != null) {
            ndnc.setFullFreshEnabled(enabled);
        }
    }

    public static boolean showNotifyDotsNum(Context context) {
        boolean showNum = false;
        NotifyDotsNumController ndnc = LauncherAppMonitor.getInstance(context).getNotifiDotsNumController();
        if (ndnc != null) {
            showNum = ndnc.isShowNotifyDotsNum();
        }
        return showNum;
    }

}

package com.sprd.ext.notificationdots;

import android.content.Context;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LauncherSettingsExtension;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import androidx.preference.Preference;

public class NotifyDotsNumController extends BaseController implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NotifyDotsNumController";
    private boolean mNeedFullFresh;
    private boolean mShowDotsNum;
    private boolean mNotifyDotsExtPrefEnable;

    private Preference mNotifyDotsExtPref;

    public NotifyDotsNumController(Context context, LauncherAppMonitor monitor) {
        super(context);
        mShowDotsNum = Utilities.getPrefs(context)
                .getBoolean(LauncherSettingsExtension.PREF_NOTIFICATION_DOTS_EXT,
                        context.getResources().getBoolean(R.bool.show_notification_dots_num));
        monitor.registerCallback(mAppMonitorCallback);
    }

    private LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            dumpState(prefix, fd, w, dumpAll);
        }
    };

    public void initPreference(Preference notifyDotsPrefExt) {
        mNotifyDotsExtPref = notifyDotsPrefExt;
        mNotifyDotsExtPref.setOnPreferenceChangeListener(this);
        mNotifyDotsExtPref.setEnabled(mNotifyDotsExtPrefEnable);
    }

    boolean isShowNotifyDotsNum() {
        return mShowDotsNum;
    }

    boolean isFullFreshEnabled() {
        return mNeedFullFresh;
    }

    void setFullFreshEnabled(boolean enabled) {
        if (mNeedFullFresh != enabled) {
            mNeedFullFresh = enabled;
        }
    }

    void setNotifyDotsExtPrefEnabled(boolean enable) {
        if (mNotifyDotsExtPrefEnable != enable) {
            mNotifyDotsExtPrefEnable = enable;
            if (null != mNotifyDotsExtPref) {
                mNotifyDotsExtPref.setEnabled(mNotifyDotsExtPrefEnable);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean show = (boolean) newValue;
        if (mShowDotsNum != show) {
            mShowDotsNum = (boolean) newValue;
            setFullFreshEnabled(true);
        }
        return true;
    }

    @Override
    public void dumpState(String prefix, FileDescriptor fd, PrintWriter writer, boolean dumpAll) {
        writer.println();
        writer.println(prefix + TAG + " mShowDotsNum:" + mShowDotsNum +
                " mNotifyDotsExtPrefEnable:" + mNotifyDotsExtPrefEnable);
        if (dumpAll) {
            Launcher launcher = LauncherAppMonitor.getInstanceNoCreate().getLauncher();
            if (launcher != null) {
                writer.println(prefix + TAG + launcher.getPopupDataProvider().dumpDotInfosMap());
            }
        }
    }
}

package com.sprd.ext.cyclescroll;

import static com.sprd.ext.LauncherSettingsExtension.PREF_CIRCULAR_SLIDE_KEY;
import static com.sprd.ext.LauncherSettingsExtension.PREF_ENABLE_MINUS_ONE;

import android.content.Context;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.sprd.ext.BaseController;
import com.sprd.ext.LauncherAppMonitor;
import com.sprd.ext.LauncherAppMonitorCallback;
import com.sprd.ext.LogUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import androidx.preference.Preference;

/**
 * Created by SPRD on 2019/5/29.
 */
public class CycleScrollController extends BaseController implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "CycleScrollController";

    private Preference mPref;
    private final LauncherAppMonitorCallback mAppMonitorCallback = new LauncherAppMonitorCallback() {
        @Override
        public void onLauncherCreated() {
            boolean enable = Utilities.getPrefs(mContext)
                    .getBoolean(PREF_CIRCULAR_SLIDE_KEY,
                            mContext.getResources().getBoolean(R.bool.default_circle_slide));
            updateCycleScrollState(enable);
        }

        @Override
        public void onAppSharedPreferenceChanged(String key) {
            if (PREF_ENABLE_MINUS_ONE.equals(key)) {
                updateCycleScrollPref();
            }
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, boolean dumpAll) {
            Launcher launcher = LauncherAppMonitor.getInstance(mContext).getLauncher();
            if (launcher != null) {
                Workspace ws = launcher.getWorkspace();
                if (ws != null) {
                    w.println();
                    w.println(prefix + TAG + ": cycle scroll:" + ws.mIsEnableCycle +
                            " enable loop:" + ws.enableLoop());
                }
            }
        }
    };

    public CycleScrollController(Context context, LauncherAppMonitor monitor) {
        super(context);
        monitor.registerCallback(mAppMonitorCallback);
    }

    public void setPref(Preference preference) {
        mPref = preference;
        preference.setOnPreferenceChangeListener(this);
        updateCycleScrollPref();
    }

    public void updateCycleScrollPref() {
        if (mPref != null) {
            boolean minusOne = Utilities.getPrefs(mContext).getBoolean(PREF_ENABLE_MINUS_ONE, false);
            String summary = minusOne ?
                    mContext.getResources().getString(R.string.allow_circular_sliding_summary) : "";
            mPref.setEnabled(!minusOne);
            mPref.setSummary(summary);
        }
    }

    private void updateCycleScrollState(boolean enable) {
        Launcher launcher = LauncherAppMonitor.getInstance(mContext).getLauncher();
        if (launcher != null) {
            Workspace ws = launcher.getWorkspace();
            if (ws != null) {
                ws.setCycleSlideEnabled(enable);
                if (LogUtils.DEBUG_ALL) {
                    LogUtils.d(TAG, "updateCycleScrollState:" + enable);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        updateCycleScrollState((Boolean) newValue);
        return true;
    }
}
